package csw.framework.models

import csw.common.TMTSerializable
import enumeratum._

import scala.collection.immutable

/**
 * This is used to tell container cmd line app to start components in either container mode or standalone mode
 */
private[framework] sealed abstract class ContainerMode extends EnumEntry with TMTSerializable

private[csw] object ContainerMode extends Enum[ContainerMode] with PlayJsonEnum[ContainerMode] {

  override def values: immutable.IndexedSeq[ContainerMode] = findValues

  case object Container  extends ContainerMode
  case object Standalone extends ContainerMode

}
