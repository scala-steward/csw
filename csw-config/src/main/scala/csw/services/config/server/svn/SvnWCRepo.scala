package csw.services.config.server.svn

import java.io.{File, InputStream, OutputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import akka.dispatch.MessageDispatcher
import csw.services.config.server.Settings
import org.tmatesoft.svn.core._
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.wc.{SVNClientManager, SVNRevision, SVNWCUtil}

import scala.collection.mutable
import scala.concurrent.Future
import scala.collection.mutable.MutableList

class SvnWCRepo(settings: Settings, blockingIoDispatcher: MessageDispatcher) extends Repo(blockingIoDispatcher) {
  private implicit val _blockingIoDispatcher = blockingIoDispatcher

  val svnClientManager: SVNClientManager = SVNClientManager.newInstance()

  def initSvnRepo(): Unit = try {
    // Create the new main repo
    FSRepositoryFactory.setup()
    val svnURL: SVNURL = SVNRepositoryFactory.createLocalRepository(settings.repositoryFile, false, false)
    // Checkout a working copy
    val options = SVNWCUtil.createDefaultOptions(true)
    val authManager = SVNWCUtil.createDefaultAuthenticationManager()
    SVNClientManager.newInstance(options, authManager).getUpdateClient.doCheckout(svnURL, Paths.get("/tmp/wc").toFile, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true)
    println(s"New Repository created at ${settings.svnUrl}")
  } catch {
    //If the repo already exists, print stracktrace and continue to boot
    case ex: SVNException if ex.getErrorMessage.getErrorCode == SVNErrorCode.IO_ERROR ⇒
      println(s"Repository already exists at ${settings.svnUrl}")
  }

  def getFile(path: Path, revision: Long, outputStream: OutputStream): Future[Unit] = Future {
    val checkedOutFile = pathFromWC(path)
    outputStream.write(Files.readAllBytes(Paths.get(checkedOutFile.toString)))
    outputStream.flush()
    outputStream.close()
  }

  private def pathFromWC(path: Path): Path = {
    Paths.get("/tmp/wc", path.toString)
  }

  def addFile(path: Path, comment: String, data: InputStream): Future[SVNCommitInfo] = Future {
    val svnClientManager: SVNClientManager = SVNClientManager.newInstance()
    try {
      val tmp = pathFromWC(path).getParent
      if (tmp != null) { // null will be returned if the path has no parent
        Files.createDirectories(tmp)
      }
      Files.copy(data, pathFromWC(path))
      svnClientManager.getCommitClient.doImport(pathFromWC(path).toFile,
        SVNURL.fromFile(Paths.get(settings.repositoryFile.getPath, path.toString).toFile), comment, null, false, false, SVNDepth.INFINITY)
//      svnClientManager.getWCClient.doAdd(pathFromWC(path).toFile, false, false, true, SVNDepth.INFINITY, true, true)
//      svnClientManager.getCommitClient.doCommit(Array(pathFromWC(path).toFile), false, comment, null, null, false, false, SVNDepth.INFINITY)
    } finally {
      svnClientManager.dispose()
    }
  }

  def modifyFile(path: Path, comment: String, data: InputStream): Future[SVNCommitInfo] = Future {
    val svnClientManager: SVNClientManager = SVNClientManager.newInstance()
    try {
      Files.copy(data, pathFromWC(path), StandardCopyOption.REPLACE_EXISTING)
      svnClientManager.getCommitClient.doCommit(Array(pathFromWC(path).toFile), false, comment, null, null, false, false, SVNDepth.INFINITY)
    } finally {
      svnClientManager.dispose()
    }
  }

  def delete(path: Path, comment: String): Future[SVNCommitInfo] = Future {
    val svnClientManager: SVNClientManager = SVNClientManager.newInstance()
    try {
      svnClientManager.getWCClient.doDelete(pathFromWC(path).toFile, true, true, false)
      svnClientManager.getCommitClient.doCommit(Array(pathFromWC(path).toFile), false, comment, null, null, false, false, SVNDepth.INFINITY)
    } finally {
      svnClientManager.dispose()
    }
  }

  def list(): Future[List[SVNDirEntry]] = Future {
    val svnClientManager: SVNClientManager = SVNClientManager.newInstance()
    try {
      var svnDirEntries: mutable.MutableList[SVNDirEntry] = null
      svnClientManager.getLogClient.doList(settings.svnUrl, SVNRevision.BASE, SVNRevision.HEAD, false, true,
        new ISVNDirEntryHandler {
          override def handleDirEntry(dirEntry: SVNDirEntry): Unit = svnDirEntries += dirEntry
        })

      svnDirEntries.toList
    } finally {
      svnClientManager.dispose()
    }
  }

  def pathExists(path: Path, id: Option[Long]): Future[Boolean] = Future {
    val svnClientManager: SVNClientManager = SVNClientManager.newInstance()
    try {
      val svnurl: File = pathFromWC(path).toFile
      val revision: SVNRevision = id.fold(SVNRevision.WORKING) {
        SVNRevision.create
      }
      svnClientManager.getWCClient.doInfo(svnurl, revision)
      true
    } catch {
      case ex: SVNException => false
    } finally {
      svnClientManager.dispose()
    }
  }

  def hist(path: Path, maxResults: Int): Future[List[SVNLogEntry]] = Future {
    val svnClientManager: SVNClientManager = SVNClientManager.newInstance()
    var logEntries = List[SVNLogEntry]()
    try {
      val logClient = svnClientManager.getLogClient
      val handler: ISVNLogEntryHandler = logEntry => logEntries = logEntry :: logEntries
      logClient.doLog(settings.svnUrl, Array(path.toString), SVNRevision.HEAD, null, null, true, true, maxResults, handler)
      logEntries.sortWith(_.getRevision > _.getRevision)
    } finally {
      svnClientManager.dispose()
    }
  } recover {
    case ex: SVNException => Nil
  }

}
