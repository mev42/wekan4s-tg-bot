package app

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

import com.typesafe.config.ConfigFactory

object Config {

  private val conf = ConfigFactory.load()

  object wecan {
    val url: String      = conf.getString("wekan.url")
    val email: String    = conf.getString("wekan.email")
    val password: String = conf.getString("wekan.password")
    val connectionTimeout: FiniteDuration = {
      val timeout = conf.getDuration("wekan.connectionTimeout")

      FiniteDuration.apply(timeout.getSeconds, TimeUnit.SECONDS)
    }
  }

  object tg {
    val token: String = conf.getString("tg.token")
    val groupId: Long = conf.getLong("tg.groupId")
  }

  object app {
    val timeout: FiniteDuration = {
      val duration = conf.getDuration("app.timeout")

      FiniteDuration.apply(duration.getSeconds, TimeUnit.SECONDS)
    }

    val lang: String = conf.getString("app.lang")
  }

}
