package csw.common.framework

import csw.common.TMTSerializable

/**
 * Represents a collection of components created in a single container
 *
 * @param components a set of components with its supervisor and componentInfo
 */
private[csw] case class Components(components: Set[Component]) extends TMTSerializable
