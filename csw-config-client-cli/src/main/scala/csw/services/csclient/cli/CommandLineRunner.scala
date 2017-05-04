package csw.services.csclient.cli

import csw.services.config.api.models.{ConfigData, ConfigId}
import csw.services.config.api.scaladsl.{ConfigAdminService, ConfigService}
import csw.services.config.client.internal.ActorRuntime

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class CommandLineRunner(configAdminService: ConfigAdminService, actorRuntime: ActorRuntime) {

  import actorRuntime._

  def run(options: Options): Unit = {

    def create(): Unit = {
      val configData = ConfigData.fromPath(options.inputFilePath.get)
      val configId =
        await(configAdminService.create(options.relativeRepoPath.get, configData, annex = options.annex,
            options.comment))
      println(s"File : ${options.relativeRepoPath.get} is created with id : ${configId.id}")
    }

    def update(): Unit = {
      val configData = ConfigData.fromPath(options.inputFilePath.get)
      val configId   = await(configAdminService.update(options.relativeRepoPath.get, configData, options.comment))
      println(s"File : ${options.relativeRepoPath.get} is updated with id : ${configId.id}")
    }

    def get(): Unit = {
      val configDataOpt = (options.date, options.id, options.latest) match {
        case (Some(date), _, _) ⇒ await(configAdminService.getByTime(options.relativeRepoPath.get, date))
        case (_, Some(id), _)   ⇒ await(configAdminService.getById(options.relativeRepoPath.get, ConfigId(id)))
        case (_, _, true)       ⇒ await(configAdminService.getLatest(options.relativeRepoPath.get))
        case (_, _, _)          ⇒ await(configAdminService.getActive(options.relativeRepoPath.get))
      }

      configDataOpt match {
        case Some(configData) ⇒
          val outputFile = await(configData.toPath(options.outputFilePath.get))
          println(s"Output file is created at location : ${outputFile.toAbsolutePath}")
        case None ⇒
      }
    }

    def exists(): Unit = {
      val exists = await(configAdminService.exists(options.relativeRepoPath.get))
      println(s"File ${options.relativeRepoPath.get} exists in the repo? : $exists")
    }

    def delete(): Unit = {
      await(configAdminService.delete(options.relativeRepoPath.get))
      println(s"File ${options.relativeRepoPath.get} deletion is completed.")
    }

    def list(): Unit = {
      val fileInfoes = await(configAdminService.list(pattern = options.pattern))
      fileInfoes.foreach(i ⇒ println(s"${i.path}\t${i.id.id}\t${i.comment}"))
    }

    def history(): Unit = {
      val histList = await(configAdminService.history(options.relativeRepoPath.get, options.maxFileVersions))
      histList.foreach(h => println(s"${h.id.id}\t${h.time}\t${h.comment}"))
    }

    def setActive(): Unit = {
      val maybeConfigId = options.id.map(id ⇒ ConfigId(id))
      await(configAdminService.setActive(options.relativeRepoPath.get, maybeConfigId.get, options.comment))
      println(s"${options.relativeRepoPath.get} file with id:${maybeConfigId.get.id} is set as default")
    }

    def resetActive(): Unit = {
      await(configAdminService.resetActive(options.relativeRepoPath.get, options.comment))
      println(s"${options.relativeRepoPath.get} file is reset to default")
    }

    options.op match {
      case "create"      ⇒ create()
      case "update"      ⇒ update()
      case "get"         ⇒ get()
      case "exists"      ⇒ exists()
      case "delete"      ⇒ delete()
      case "list"        ⇒ list()
      case "history"     ⇒ history()
      case "setActive"   ⇒ setActive()
      case "resetActive" ⇒ resetActive()
      case x             ⇒ throw new RuntimeException(s"Unknown operation: $x")
    }
  }

  //command line app is by nature blocking.
  //Do not use such method in library/server side code
  private def await[T](future: Future[T]): T = Await.result(future, Duration.Inf)
}
