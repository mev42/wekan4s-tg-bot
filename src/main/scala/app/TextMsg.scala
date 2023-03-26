package app

import app.model.RawWekanData
import com.osinka.i18n.{Lang, Messages}

object TextMsg {

  private implicit val userLang: Lang = Lang(Config.app.lang)

  val botWasStarted: String = Messages("botWasStarted")
  val board: String         = Messages("board")
  val list: String          = Messages("list")
  val card: String          = Messages("card")
  val comment: String       = Messages("comment")
  val cancel: String        = Messages("cancel")
  val chooseABoard: String  = Messages("chooseABoard")
  val loading: String       = Messages("loading")

  def rawWekanData2msg(rwd: RawWekanData): String = {
    List(
      Some(s"$board: ${rwd.boardTitle}"),
      Some(s"$list: ${rwd.listTitle}"),
      rwd.cardTitle.map(ct => s"$card: $ct"),
      rwd.comment.map(c => s"$comment $c")
    ).collect { case Some(v) => v }.mkString("\n")
  }

}
