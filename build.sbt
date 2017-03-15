val enableCoverage = System.getProperty("enableCoverage", "true")
val plugins:Seq[Plugins] = if(enableCoverage.toBoolean) Seq(Coverage) else Seq.empty

lazy val csw = project
  .in(file("."))
  .enablePlugins(UnidocSite, PublishGithub, GitBranchPrompt)
  .aggregate(`csw-location`, `trackLocation`, docs)
  .settings(Settings.mergeSiteWith(docs))


lazy val `csw-location` = project
  .enablePlugins(PublishBintray)
  .enablePlugins(plugins:_*)
  .settings(
    libraryDependencies ++= Seq(
      Akka.`akka-stream`,
      Akka.`akka-distributed-data-experimental`,
      Libs.`jmdns`,
      Libs.`scala-java8-compat`,
      Libs.`scala-async`,
      Libs.`chill-akka`,
      Libs.`enumeratum`
    ),
    libraryDependencies ++= Seq(
      Akka.`akka-stream-testkit` % Test,
      Libs.`scalatest` % Test,
      Libs.`scalamock-scalatest-support` % Test,
      Libs.`junit` % Test,
      Libs.`junit-interface` % Test
    )
  )

lazy val `trackLocation` = project
  .in(file("apps/trackLocation"))
  .enablePlugins(DeployApp)
  .dependsOn(`csw-location`)
  .settings(
    libraryDependencies ++= Seq(
      Akka.`akka-actor`,
      Libs.`scopt`,
      Libs.`scalatest` % Test,
      Libs.`scalamock-scalatest-support` % Test,
      Libs.`scala-logging` % Test
    )
  )

lazy val docs = project
  .enablePlugins(ParadoxSite, NoPublish)
