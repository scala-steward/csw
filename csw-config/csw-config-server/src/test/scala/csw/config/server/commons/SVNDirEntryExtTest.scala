package csw.config.server.commons

import java.util.Date
import java.util.regex.Pattern

import csw.config.models.FileType
import csw.config.server.{ServerWiring, Settings}
import csw.config.server.commons.SVNDirEntryExt.RichSvnDirEntry
import org.jboss.netty.logging.{InternalLoggerFactory, Slf4JLoggerFactory}
import org.tmatesoft.svn.core.{SVNDirEntry, SVNNodeKind}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SVNDirEntryExtTest extends AnyFunSuite with Matchers {
  val settings: Settings = new ServerWiring().settings

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  test("should match the pattern for relative path") {
    val dirEntry = new SVNDirEntry(
      settings.svnUrl,
      settings.svnUrl,
      "a/b/sample.txt",
      SVNNodeKind.FILE,
      100,
      false,
      1,
      new Date(),
      "author",
      "comment"
    )

    val patterns = List(
      Pattern.compile("a/.*"),
      Pattern.compile("a/b.*"),
      Pattern.compile("a/b/sample.*"),
      Pattern.compile(".*/b/sample.*"),
      Pattern.compile(".*sample.*"),
      Pattern.compile(".*.txt"),
      Pattern.compile(".*txt"),
      Pattern.compile(".*sample.txt.*")
    )

    dirEntry.isFile shouldBe true
    dirEntry.isNotActiveFile(settings.`active-config-suffix`) shouldBe true

    patterns.foreach(pattern => dirEntry.matches(Some(pattern)) shouldBe true)
  }

  test("should not match invalid pattern for relative path") {
    val dirEntry = new SVNDirEntry(
      settings.svnUrl,
      settings.svnUrl,
      "a/b/sample.txt",
      SVNNodeKind.FILE,
      100,
      false,
      1,
      new Date(),
      "author",
      "comment"
    )

    val patterns = List(
      Pattern.compile(""),
      Pattern.compile("invalidstring")
    )

    patterns.foreach(pattern => dirEntry.matches(Some(pattern)) shouldBe false)
  }

  test("should detect annex and normal file based on type") {
    val normalDirEntry = new SVNDirEntry(
      settings.svnUrl,
      settings.svnUrl,
      "a/b/sample.txt",
      SVNNodeKind.FILE,
      100,
      false,
      1,
      new Date(),
      "author",
      "comment"
    )

    val annexDirEntry = new SVNDirEntry(
      settings.svnUrl,
      settings.svnUrl,
      "a/b/sample.txt.$sha1",
      SVNNodeKind.FILE,
      100,
      false,
      1,
      new Date(),
      "author",
      "comment"
    )

    normalDirEntry.matchesFileType(Some(FileType.Normal), settings.`sha1-suffix`) shouldBe true
    annexDirEntry.matchesFileType(Some(FileType.Annex), settings.`sha1-suffix`) shouldBe true
  }
}
