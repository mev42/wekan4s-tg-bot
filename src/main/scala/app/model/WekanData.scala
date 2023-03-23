package app.model

import cats.implicits._

case class ListCard(card: GetAllCardsResponse, comments: List[GetAllCommentsResponse])

case class BoardList(list: GetAllListsResponse, cards: List[ListCard])

case class Board(board: GetBoardResponse, lists: List[BoardList])

//Map[Id, Board]
case class WekanData(boards: Map[String, Board]) {
  def asRawWekanDatas: List[RawWekanData] = {
    (for {
      board <- boards.values.toList
      list  <- board.lists
      emptyList = RawWekanData(
        boardId = board.board._id,
        boardTitle = board.board.title,
        listId = list.list._id,
        listTitle = list.list.title
      )
      emptyCards = emptyList :: list.cards.map(lc =>
        RawWekanData(
          boardId = board.board._id,
          boardTitle = board.board.title,
          listId = list.list._id,
          listTitle = list.list.title,
          cardId = lc.card._id.some,
          cardTitle = lc.card.title,
          cardDescription = lc.card.description
        )
      )
      card    <- list.cards
      comment <- card.comments
    } yield {
      RawWekanData(
        boardId = board.board._id,
        boardTitle = board.board.title,
        listId = list.list._id,
        listTitle = list.list.title,
        cardId = card.card._id.some,
        cardTitle = card.card.title,
        cardDescription = card.card.description,
        comment = comment.comment.some,
        commentId = comment._id.some,
        authorId = comment._id.some
      ) :: emptyCards
    }).flatten.distinct
  }
}

case class RawWekanData(
    boardId: String,
    boardTitle: String,
    listId: String,
    listTitle: String,
    cardId: Option[String] = None,
    cardTitle: Option[String] = None,
    cardDescription: Option[String] = None,
    commentId: Option[String] = None,
    comment: Option[String] = None,
    authorId: Option[String] = None
)
