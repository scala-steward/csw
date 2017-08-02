package csw.common.components.assembly

import csw.common.framework.models.RunningMsg.DomainMsg

sealed trait AssemblyDomainMessages extends DomainMsg
case object OperationsMode          extends AssemblyDomainMessages
