package app

import app.model.GetAllCardsResponse
import app.model.GetAllCommentsResponse
import app.model.GetAllListsResponse
import app.model.GetAllSwimlanesResponse
import app.model.GetBoardResponse
import app.model.LoginResponse
import app.model.NewCardBody
import app.model.NewCardResponse
import app.model.NewCommentBody
import app.model.NewCommentResponse
import app.model.NewListBody
import app.model.NewListResponse
import app.model.User
import cats.effect.Async
import cats.implicits._
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import sttp.client3._

class Wekan[F[_]](b: SttpBackend[F, Any], loginResponse: LoginResponse)(implicit F: Async[F]) {

  private val api     = Config.wecan.url
  private val headers = Map("Content-Type" -> "application/json", "Authorization" -> s"Bearer ${loginResponse.token}")

  val admin: LoginResponse = loginResponse

  object Boards {
    def getPublicBoards: F[List[GetBoardResponse]] = {
      quickRequest
        .get(uri"$api/api/boards")
        .headers(headers)
        .send(b)
        .asProduct[List[GetBoardResponse]]
    }

    def getBoardsFromUser(user: String): F[List[GetBoardResponse]] = {
      quickRequest
        .get(uri"$api/api/users/${user}/boards")
        .headers(headers)
        .send(b)
        .asProduct[List[GetBoardResponse]]
    }

    // todo
    def getBoard(id: String): F[GetBoardResponse] = {
      quickRequest
        .get(uri"$api/api/boards/$id")
        .headers(headers)
        .send(b)
        .asProduct[GetBoardResponse]
    }
  }

  object Lists {
    def getAllLists(
        boardId: String
    ): F[List[GetAllListsResponse]] = {
      quickRequest
        .get(uri"$api/api/boards/${boardId}/lists")
        .headers(headers)
        .send(b)
        .asProduct[List[GetAllListsResponse]]
    }

    def newList(
        boardId: String,
        newListBody: NewListBody
    ): F[NewListResponse] = {
      quickRequest
        .post(uri"$api/api/boards/${boardId}/lists")
        .headers(headers)
        .body(newListBody.asJson.toString())
        .send(b)
        .asProduct[NewListResponse]
    }
  }

  object Cards {

    def getAllCards(boardId: String, listId: String): F[List[GetAllCardsResponse]] = {
      quickRequest
        .get(uri"$api/api/boards/${boardId}/lists/${listId}/cards")
        .headers(headers)
        .send(b)
        .asProduct[List[GetAllCardsResponse]]
    }

    def newCard(boardId: String, listId: String, newCardBody: NewCardBody): F[NewCardResponse] = {
      quickRequest
        .post(uri"$api/api/boards/${boardId}/lists/${listId}/cards")
        .headers(headers)
        .body(newCardBody.asJson.toString())
        .send(b)
        .asProduct[NewCardResponse]
    }
  }

  object CardsComments {

    def getAllComments(boardId: String, cardId: String): F[List[GetAllCommentsResponse]] = {
      quickRequest
        .get(uri"$api/api/boards/${boardId}/cards/${cardId}/comments")
        .headers(headers)
        .send(b)
        .asProduct[List[GetAllCommentsResponse]]
    }

    def newComment(boardId: String, cardId: String, comment: String): F[NewCommentResponse] = {
      val newCommentBody = NewCommentBody(loginResponse.id, comment)

      quickRequest
        .post(uri"$api/api/boards/${boardId}/cards/${cardId}/comments")
        .body(newCommentBody.asJson.toString())
        .headers(headers)
        .send(b)
        .asProduct[NewCommentResponse]
    }
  }

  object Users {

    def getUser(id: String): F[User] = {
      quickRequest
        .post(uri"$api/api/users/$id")
        .headers(headers)
        .send(b)
        .asProduct[User]
    }

  }

  object Swimlanes {
    def getAllSwimlanes(boardId: String): F[List[GetAllSwimlanesResponse]] = {
      quickRequest
        .get(uri"$api/api/boards/$boardId/swimlanes")
        .headers(headers)
        .send(b)
        .asProduct[List[GetAllSwimlanesResponse]]
    }
  }

  private implicit class Resp2product(response: F[Response[String]]) {
    def asProduct[A: Decoder]: F[A] = {
      response
        .map(r => parse(r.body).flatMap(_.as[A]).leftMap(_.toString))
        .flatMap(e => F.fromEither(e.leftMap(msg => new Exception(msg))))
    }
  }
}

object Wekan {

  def apply[F[_]: Async](b: SttpBackend[F, Any], loginResponse: LoginResponse) = new Wekan[F](b, loginResponse)

  object Login {

    private case class EmailPassword(email: String, password: String)

    def login[F[_]]()(implicit F: Async[F], b: SttpBackend[F, Any]): F[LoginResponse] = {
      quickRequest
        .post(uri"${Config.wecan.url}/users/login")
        .headers(Map("Content-Type" -> "application/json"))
        .body(EmailPassword(Config.wecan.email, Config.wecan.password).asJson.toString())
        .send(b)
        .map { r =>
          parse(r.body).flatMap(_.as[LoginResponse]).leftMap(_.toString)
        }
        .flatMap(e => F.fromEither(e.leftMap(msg => new Exception(msg))))
    }
  }
}
