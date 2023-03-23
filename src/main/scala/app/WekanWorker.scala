package app

import app.model.Board
import app.model.BoardList
import app.model.ListCard
import app.model.RawWekanData
import app.model.WekanData
import cats.effect.Async
import cats.effect.Ref
import cats.implicits._

class WekanWorker[F[_]: Async](api: Wekan[F], data: Ref[F, WekanData]) {

  def init(boardIds: List[String]): F[WekanData] = {
    for {
      boards <- boardIds.traverse(api.Boards.getBoard(_))
      boardsData <- boards.traverse { board =>
        for {
          lists <- api.Lists.getAllLists(board._id)
          listsData <- lists.traverse { list =>
            for {
              cards <- api.Cards.getAllCards(board._id, list._id)
              cardData <- cards.traverse { card =>
                for {
                  comments <- api.CardsComments.getAllComments(board._id, card._id)
                } yield ListCard(card, comments)
              }
            } yield BoardList(list, cardData)
          }
        } yield Board(board, listsData)
      }
      wekanMap = boardsData.map(d => (d.board._id, d)).toMap
    } yield WekanData(wekanMap)
  }

  def checkAndUpdate(boardIds: List[String]): F[List[RawWekanData]] = {
    for {
      newWekanData <- init(boardIds)
      currentData  <- data.get.map(_.asRawWekanDatas)
      updates = newWekanData.asRawWekanDatas.diff(currentData)
      _ <- data.updateAndGet(_ => newWekanData)
    } yield updates
  }

}

object WekanWorker {
  def apply[F[_]: Async](api: Wekan[F], data: Ref[F, WekanData]) = new WekanWorker[F](api, data)
}