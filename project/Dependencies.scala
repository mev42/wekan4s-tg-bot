import sbt._

object Dependencies {

  lazy val sttpVersion = "3.7.6"

  val root: Seq[ModuleID] = Seq(
    "org.typelevel"                 %% "cats-effect"                    % "3.3.14",
    "ch.qos.logback"                 % "logback-classic"                % "1.2.3",
    "net.logstash.logback"           % "logstash-logback-encoder"       % "5.3",
    "com.typesafe"                   % "config"                         % "1.3.3",
    "com.softwaremill.sttp.client3" %% "core"                           % sttpVersion,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % sttpVersion,
    "org.augustjune"                %% "canoe"                          % "0.6.0",
    "com.osinka.i18n"               %% "scala-i18n"                     % "1.0.3"
  )

}
