package app

import scala.concurrent.duration.DurationInt
import app.model.Board
import app.model.GetBoardResponse
import app.model.NewCardBody
import app.model.NewListBody
import app.model.RawWekanData
import app.model.WekanData
import canoe.api._
import canoe.api.models.Keyboard
import canoe.methods.messages.SendMessage
import canoe.models.Chat
import canoe.models.KeyboardButton
import canoe.models.ReplyKeyboardMarkup
import canoe.models.messages.TextMessage
import canoe.syntax._
import cats.effect.{Async, ExitCode, IO, IOApp, OutcomeIO, Ref, Resource}
import cats.implicits._
import fs2.Stream
import sttp.client3.SttpBackend
import sttp.client3.SttpBackendOptions
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

object Boot extends IOApp {

  def notifier(ww: WekanWorker[IO], boards: List[GetBoardResponse], tg: TelegramClient[IO]): IO[Unit] = {
    for {
      _    <- IO.sleep(Config.app.timeout)
      rwds <- ww.checkAndUpdate(boards.map(_._id))
      _    <- rwds.traverse(rwd => sendText[IO](Config.tg.groupId, TextMsg.rawWekanData2msg(rwd))(tg))
    } yield ()
  }

  val app: Resource[IO, Unit] = {
    for {
      b     <- AsyncHttpClientCatsBackend.resource[IO]()
      tg    <- TelegramClient[IO](Config.tg.token)
      login <- Resource.eval(Wekan.Login.login[IO]()(IO.asyncForIO, b))
      wekanApi = Wekan[IO](b, login)
      wekanData <- Resource.eval(Ref[IO].of(WekanData(Map.empty[String, Board])))
      wekanWorker = WekanWorker[IO](wekanApi, wekanData)
      boards <- Resource.eval(wekanApi.Boards.getPublicBoards)
      data   <- Resource.eval(wekanWorker.init(boards.map(_._id)))
      _      <- Resource.eval(wekanData.updateAndGet(_ => data))
      _      <- Resource.eval(sendText[IO](Config.tg.groupId, TextMsg.botWasStarted)(tg))
      _      <- notifier(wekanWorker, boards, tg).foreverM.start.background
      _ <- Stream
        .emit(tg)
        .flatMap(implicit client => Bot.polling[IO].follow(onStart(wekanApi)))
        .compile
        .drain
        .background
    } yield ()
  }

  def run(args: List[String]): IO[ExitCode] =
    app.use(_ => IO.never).as(ExitCode.Success)

  def sendText[F[_]: TelegramClient](chatId: Long, text: String): F[TextMessage] =
    SendMessage(chatId, text).call

  def onStart[F[_]: TelegramClient: Async](wekan: Wekan[F]): Scenario[F, Unit] = {
    for {
      chat  <- Scenario.expect(command("start").chat)
      board <- processBoard[F](chat, wekan)
      _     <- processList(chat, wekan, board._id)
      _     <- Scenario.done
    } yield ()
  }

  def processBoard[F[_]: TelegramClient: Async](chat: Chat, wekan: Wekan[F]): Scenario[F, GetBoardResponse] = {
    for {
      _      <- Scenario.eval(chat.send(TextMsg.loading))
      boards <- Scenario.eval(wekan.Boards.getPublicBoards)
      boardsKeyboards = ReplyKeyboardMarkup(keyboard = boards.map { board =>
        Seq(KeyboardButton(board.title))
      })
      _         <- Scenario.eval(chat.send(TextMsg.chooseABoard, keyboard = Keyboard.Reply(boardsKeyboards)))
      boardName <- Scenario.expect(text)
      board = boards.find(_.title == boardName).getOrElse(boards.head)
      _ <- Scenario.eval(chat.send(s"Доска: ${board.title}"))
    } yield board
  }

  def processList[F[_]: TelegramClient: Async](chat: Chat, wekan: Wekan[F], boardId: String): Scenario[F, Unit] = {
    for {
      lists <- Scenario.eval(wekan.Lists.getAllLists(boardId))
      listsKeyboard = ReplyKeyboardMarkup(keyboard = lists.map { list =>
        Seq(KeyboardButton(list.title + s"<${list._id}>"))
      } :+ Seq(KeyboardButton("Добавить новый список")))
      _         <- Scenario.eval(chat.send("Списки:", keyboard = Keyboard.Reply(listsKeyboard)))
      actionMsg <- Scenario.expect(text)
      _ <- {
        if (actionMsg == "Добавить новый список") {
          processNewBoardList[F](chat, wekan, boardId)
        } else {
          val list = lists.find(l => actionMsg.contains(s"<${l._id}>")).get
          processCards[F](chat, wekan, boardId, list._id)
        }
      }
    } yield ()
  }

  def processCards[F[_]: TelegramClient: Async](
      chat: Chat,
      wekan: Wekan[F],
      boardId: String,
      listId: String
  ): Scenario[F, Unit] = {
    for {
      cards <- Scenario.eval(wekan.Cards.getAllCards(boardId, listId))
      listsKeyboard = ReplyKeyboardMarkup(keyboard = cards.map { list =>
        Seq(KeyboardButton(list.title.getOrElse("") + s"<${list._id}>"))
      } :+ Seq(KeyboardButton("Добавить новую карточку")))
      _         <- Scenario.eval(chat.send("Карточки:", keyboard = Keyboard.Reply(listsKeyboard)))
      actionMsg <- Scenario.expect(text)
      _ <- {
        if (actionMsg == "Добавить новую карточку") {
          processNewCard[F](chat, wekan, boardId, listId)
        } else {
          val list = cards.find(l => actionMsg.contains(s"<${l._id}>")).get
          processCardComments[F](chat, wekan, boardId, list._id)
        }
      }
    } yield ()
  }

  def processNewBoardList[F[_]: TelegramClient: Async](
      chat: Chat,
      wekan: Wekan[F],
      boardId: String
  ): Scenario[F, Unit] = {
    for {
      _            <- Scenario.eval(chat.send("Введите название нового списка", keyboard = Keyboard.Remove))
      newListTitle <- Scenario.expect(text)
      _            <- Scenario.eval(wekan.Lists.newList(boardId, NewListBody(newListTitle)))
      _            <- Scenario.eval(chat.send("Успешно"))
    } yield ()
  }

  def processNewCard[F[_]: TelegramClient: Async](
      chat: Chat,
      wekan: Wekan[F],
      boardId: String,
      listId: String
  ): Scenario[F, Unit] = {
    for {
      swimlanes          <- Scenario.eval(wekan.Swimlanes.getAllSwimlanes(boardId))
      _                  <- Scenario.eval(chat.send("Введите название карточки", keyboard = Keyboard.Remove))
      newCardTitle       <- Scenario.expect(text)
      _                  <- Scenario.eval(chat.send("Введите описание карточки"))
      newCardDescription <- Scenario.expect(text)
      newCardBody = NewCardBody(wekan.admin.id, None, newCardTitle, newCardDescription, swimlanes.head._id)
      _ <- Scenario.eval(wekan.Cards.newCard(boardId, listId, newCardBody))
      _ <- Scenario.eval(chat.send("Успешно"))
    } yield ()
  }

  def processCardComments[F[_]: TelegramClient: Async](
      chat: Chat,
      wekan: Wekan[F],
      boardId: String,
      cardId: String
  ): Scenario[F, Unit] = {
    for {
      _        <- Scenario.eval(chat.send("Комментарии:", keyboard = Keyboard.Remove))
      comments <- Scenario.eval(wekan.CardsComments.getAllComments(boardId, cardId))
      msg = comments.map(_.comment).mkString("<>\n", "\n", "\n<>")
      _ <- Scenario.eval(chat.send(msg))
    } yield ()
  }

}
