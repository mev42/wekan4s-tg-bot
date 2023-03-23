lazy val root = (project in file("."))
  .settings(
    name         := "wekan-tg-bot",
    scalaVersion := "2.13.8",
    libraryDependencies ++= Dependencies.root,
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalafixOnCompile := true,
    scalacOptions ++= List("-Wunused:imports"),
    assembly / assemblyMergeStrategy := {
      case path if path.endsWith("module-info.class")                     => MergeStrategy.last
      case path if path.endsWith("META-INF/git.properties")               => MergeStrategy.last
      case path if path.endsWith("META-INF/io.netty.versions.properties") => MergeStrategy.last
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
