package app

import com.osinka.i18n.{Lang, Messages}

object TextMsg {

  private implicit val userLang = Lang(Config.app.lang)

  val botWasStarted: String = Messages("botWasStarted")



}
