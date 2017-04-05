package csw.services.cs.core

import java.io.File
import java.net.URI

import akka.actor.{ActorRefFactory, ActorSystem}
import csw.services.cs.core.svn.SvnConfigManager
import org.scalatest.FunSuite
import TestFutureExtension.RichFuture
import akka.stream.ActorMaterializer

class ConfigManagerTest extends FunSuite {

  val path1 = new File("some/test1/TestConfig1")
  val path2 = new File("some/test2/TestConfig2")

  val contents1 = "Contents of some file...\n"
  val contents2 = "New contents of some file...\n"
  val contents3 = "Even newer contents of some file...\n"

  val comment1 = "create comment"
  val comment2 = "update 1 comment"
  val comment3 = "update 2 comment"

  implicit val system = ActorSystem("test")
  implicit val mat = ActorMaterializer()

  val svnRepo = new URI(system.settings.config.getString("csw.services.cs.main-repository"))
  private def resetRepo()(implicit context: ActorRefFactory): Unit = {
    // XXX FIXME TODO: Use generated temp dirs, not settings
    //    println(s"Using test svn repo at = ${settings.mainRepository}")
    if (svnRepo.getScheme != "file")
      throw new RuntimeException(s"Please specify a file URI for csw.services.cs.main-repository for testing")

    val svnMainRepo = new File(svnRepo.getPath)
    // Delete the main and local test repositories (Only use this in test cases!)
    SvnConfigManager.deleteDirectoryRecursively(svnMainRepo)
    SvnConfigManager.initSvnRepo(svnMainRepo)
  }

  test("svn file is created") {

    resetRepo()
    val manager = SvnConfigManager(svnRepo, "svn-repo")

    val configId = manager.create(path1, ConfigData(contents1), false, comment1).await

    assert(manager.get(path1).await.get.toString == contents1)

    println(configId)
  }

}
