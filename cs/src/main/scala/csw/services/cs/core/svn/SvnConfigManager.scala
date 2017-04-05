package csw.services.cs.core.svn

import java.io._
import java.net.URI

import akka.actor.ActorRefFactory
import csw.services.cs.core._
import org.tmatesoft.svn.core._
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl
import org.tmatesoft.svn.core.io.{SVNRepository, SVNRepositoryFactory}
import org.tmatesoft.svn.core.wc.SVNRevision

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Used to initialize an instance of SvnConfigManager with a given repository directory
 */
object SvnConfigManager {

  private val tmpDir = System.getProperty("java.io.tmpdir")

  // $file.default holds the id of the default version of file
  private val defaultSuffix = ".default"

  // $file.sha1 holds the SHA-1 hash of oversize files that are stored on the config service annex http server
  private val sha1Suffix = ".sha1"

  /**
   * Creates and returns a SvnConfigManager instance using the given
   * URI as the remote, central Svn repository.
   *
   * @param svnRepo the URI of the remote svn repository
   * @param name    the name of this service
   * @return a new SvnConfigManager configured to use the given remote repository
   */
  def apply(svnRepo: URI, name: String = "Config Service")(implicit context: ActorRefFactory): SvnConfigManager = {

    //Set up connection protocols support:
    //http:// and https://
    DAVRepositoryFactory.setup()
    //svn://, svn+xxx:// (svn+ssh:// in particular)
    SVNRepositoryFactoryImpl.setup()
    //file:///
    FSRepositoryFactory.setup()

    val url = SVNURL.parseURIEncoded(svnRepo.toString)
    new SvnConfigManager(url, name)
  }

  /**
   * FOR TESTING: Deletes the contents of the given directory (recursively).
   * This is meant for use by tests that need to always start with an empty Svn repository.
   */
  def deleteDirectoryRecursively(dir: File): Unit = {
    // just to be safe, don't delete anything that is not in /tmp/
    val p = dir.getPath
    if (!p.startsWith("/tmp/") && !p.startsWith(tmpDir))
      throw new RuntimeException(s"Refusing to delete $dir since not in /tmp/ or $tmpDir")

    if (dir.isDirectory) {
      dir.list.foreach {
        filePath =>
          val file = new File(dir, filePath)
          if (file.isDirectory) {
            deleteDirectoryRecursively(file)
          } else {
            file.delete()
          }
      }
      dir.delete()
    }
  }

  /**
   * Initializes an svn repository in the given dir.
   *
   * @param dir directory to contain the new repository
   */
  def initSvnRepo(dir: File)(implicit context: ActorRefFactory): Unit = {
    // Create the new main repo
    FSRepositoryFactory.setup()
    SVNRepositoryFactory.createLocalRepository(dir, false, true)
  }
}

/**
 * Uses JSvn to manage versions of configuration files.
 * Special handling is available for large/binary files (oversize option in create).
 * Oversize files can be stored on an "annex" server using the SHA-1 hash of the file
 * contents for the name (similar to the way Svn stores file objects).
 * (Note: This special handling is probably not necessary when using svn)
 *
 * @param url  used to access the svn repository
 * @param name the name of the service
 */
class SvnConfigManager(val url: SVNURL, override val name: String)(implicit context: ActorRefFactory)
    extends ConfigManager {

  override def create(path: File, configData: ConfigData, oversize: Boolean, comment: String): Future[ConfigId] = {

    // If the file does not already exists in the repo, create it
    def createImpl(present: Boolean): Future[ConfigId] = {
      if (present) {
        Future.failed(new IOException("File already exists in repository: " + path))
      } else {
        put(path, configData, update = false, comment)
      }
    }

    for {
      present <- exists(path)
      configId <- createImpl(present)
    } yield configId
  }

  override def exists(path: File): Future[Boolean] = Future(pathExists(path))

  override def get(path: File, id: Option[ConfigId]): Future[Option[ConfigData]] = {


    // Returns the contents of the given version of the file, if found
    def getConfigData: Future[Option[ConfigData]] = Future {
      val os = new ByteArrayOutputStream()
      val svn = getSvn
      try {
        svn.getFile(path.getPath, svnRevision(id).getNumber, null, os)
      } finally {
        svn.closeSession()
      }
      Some(ConfigData(os.toByteArray))
    }

    // If the file exists in the repo, get its data
    def getImpl(present: Boolean): Future[Option[ConfigData]] = {
      if (!present) {
        Future(None)
      } else {
        getConfigData
      }
    }

    // -- svn get --
    for {
      present <- exists(path)
      configData <- getImpl(present)
    } yield configData
  }

  private def svnRevision(id: Option[ConfigId] = None): SVNRevision = {
    id match {
      case Some(configId) => SVNRevision.create(configId.id.toLong)
      case None           => SVNRevision.HEAD
    }
  }

  private def pathExists(path: File): Boolean = {
    val svn = getSvn
    try {
      svn.checkPath(path.getPath, SVNRepository.INVALID_REVISION) == SVNNodeKind.FILE
    } finally {
      svn.closeSession()
    }
  }

  private def getSvn: SVNRepository = {
    val svn = SVNRepositoryFactory.create(url)
    val authManager = BasicAuthenticationManager.newInstance(getUserName, Array[Char]())
    svn.setAuthenticationManager(authManager)
    svn
  }

  private def getUserName: String = {
    System.getProperty("user.name")
  }

  private def put(path: File, configData: ConfigData, update: Boolean, comment: String = ""): Future[ConfigId] = {
    val os = new ByteArrayOutputStream()
    for {
      _ <- configData.writeToOutputStream(os)
    } yield {
      val data = os.toByteArray
      val commitInfo = if (update) {
        modifyFile(comment, path, data)
      } else {
        addFile(comment, path, data)
      }
      ConfigId(commitInfo.getNewRevision)
    }
  }

  // XXX The code below worked, but might need some work (see closeDir below), Using borrowed CommitBuilder class for now
  import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator
  // Adds the given file (and dir if needed) to svn.
  // See http://svn.svnkit.com/repos/svnkit/tags/1.3.5/doc/examples/src/org/tmatesoft/svn/examples/repository/Commit.java.
  private def addFile(comment: String, path: File, data: Array[Byte]): SVNCommitInfo = {
    val svn = getSvn
    try {
      val editor = svn.getCommitEditor(comment, null)
      editor.openRoot(SVNRepository.INVALID_REVISION)
      val dirPath = path.getParentFile
      // Recursively add any missing directories leading to the file
      def addDir(dir: File): Unit = {
        if (dir != null) {
          addDir(dir.getParentFile)
          if (!dirExists(dir)) {
            editor.addDir(dir.getPath, null, SVNRepository.INVALID_REVISION)
          }
        }
      }
      addDir(dirPath)
      val filePath = path.getPath
      editor.addFile(filePath, null, SVNRepository.INVALID_REVISION)
      editor.applyTextDelta(filePath, null)
      val deltaGenerator = new SVNDeltaGenerator
      val checksum = deltaGenerator.sendDelta(filePath, new ByteArrayInputStream(data), editor, true)
      editor.closeFile(filePath, checksum)
      editor.closeDir() // XXX TODO I think all added parent dirs need to be closed also
      editor.closeEdit()
    } finally {
      svn.closeSession()
    }
  }

  // Modifies the contents of the given file in the repository.
  // See http://svn.svnkit.com/repos/svnkit/tags/1.3.5/doc/examples/src/org/tmatesoft/svn/examples/repository/Commit.java.
  def modifyFile(comment: String, path: File, data: Array[Byte]): SVNCommitInfo = {
    val svn = getSvn
    try {
      val editor = svn.getCommitEditor(comment, null)
      editor.openRoot(SVNRepository.INVALID_REVISION)
      val filePath = path.getPath
      editor.openFile(filePath, SVNRepository.INVALID_REVISION)
      editor.applyTextDelta(filePath, null)
      val deltaGenerator = new SVNDeltaGenerator
      val checksum = deltaGenerator.sendDelta(filePath, new ByteArrayInputStream(data), editor, true)
      editor.closeFile(filePath, checksum)
      editor.closeDir()
      editor.closeEdit
    } finally {
      svn.closeSession()
    }
  }

  // True if the directory path exists in the repository
  private def dirExists(path: File): Boolean = {
    val svn = getSvn
    try {
      svn.checkPath(path.getPath, SVNRepository.INVALID_REVISION) == SVNNodeKind.DIR
    } finally {
      svn.closeSession()
    }
  }
}
