package csw.vslice.assembly

import akka.actor.Props
import akka.typed.Behavior
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.ActorContext
import csw.param.Events.{StatusEvent, SystemEvent}
import csw.param._
import csw.vslice.assembly.TrombonePublisher._
import csw.vslice.assembly.TromboneStateActor.TromboneState

class TrombonePublisher(assemblyContext: AssemblyContext, ctx: ActorContext[TromboneState])
    extends MutableBehavior[TrombonePublisherMsg] {
  import TromboneStateActor._
  import assemblyContext._

  override def onMessage(msg: TrombonePublisherMsg): Behavior[TrombonePublisherMsg] = msg match {
    case AOESWUpdate(elevationItem, rangeItem) =>
      publishAOESW(elevationItem, rangeItem)
      this

    case EngrUpdate(rtcFocusError, stagePosition, zenithAngle) =>
      publishEngr(rtcFocusError, stagePosition, zenithAngle)
      this

    case TrombonePublisherMsgE(ts) =>
      publishState(ts)
      this

    case AxisStateUpdate(axisName, position, state, inLowLimit, inHighLimit, inHome) =>
      publishAxisState(axisName, position, state, inLowLimit, inHighLimit, inHome)
      this

    case AxisStatsUpdate(axisName,
                         datumCount,
                         moveCount,
                         homeCount,
                         limitCount,
                         successCount,
                         failureCount,
                         cancelCount) =>
      publishAxisStats(axisName, datumCount, moveCount, homeCount, limitCount, successCount, failureCount, cancelCount)
      this

  }

  private def publishAOESW(elevationItem: DoubleParameter, rangeItem: DoubleParameter) = {
    val se = SystemEvent(aoSystemEventPrefix).madd(elevationItem, rangeItem)
//    eventService.foreach(_.publish(se).onComplete {
//      case Failure(ex) => log.error("TrombonePublisher failed to publish AO system event: $se", ex)
//      case _           =>
//    })
  }

  private def publishEngr(rtcFocusError: DoubleParameter,
                          stagePosition: DoubleParameter,
                          zenithAngle: DoubleParameter) = {
    val ste = StatusEvent(engStatusEventPrefix).madd(rtcFocusError, stagePosition, zenithAngle)
//    telemetryService.foreach(_.publish(ste).onComplete {
//      case Failure(ex) => log.error(s"TrombonePublisher failed to publish engr event: $ste", ex)
//      case _           =>
//    })
  }

  private def publishState(ts: TromboneState) = {
    // We can do this for convenience rather than using TromboneStateHandler's stateReceive
    val ste = StatusEvent(tromboneStateStatusEventPrefix).madd(ts.cmd, ts.move, ts.sodiumLayer, ts.nss)
//    telemetryService.foreach(_.publish(ste).onComplete {
//      case Failure(ex) => log.error(s"TrombonePublisher failed to publish trombone state: $ste", ex)
//      case _           =>
//    })
  }

  private def publishAxisState(axisName: StringParameter,
                               position: IntParameter,
                               state: ChoiceParameter,
                               inLowLimit: BooleanParameter,
                               inHighLimit: BooleanParameter,
                               inHome: BooleanParameter) = {
    val ste = StatusEvent(axisStateEventPrefix).madd(axisName, position, state, inLowLimit, inHighLimit, inHome)
//    log.debug(s"Axis state publish of $axisStateEventPrefix: $ste")
//    telemetryService.foreach(_.publish(ste).onComplete {
//      case Failure(ex) => log.error(s"TrombonePublisher failed to publish trombone axis state: $ste", ex)
//      case _           =>
//    })
  }

  def publishAxisStats(axisName: StringParameter,
                       datumCount: IntParameter,
                       moveCount: IntParameter,
                       homeCount: IntParameter,
                       limitCount: IntParameter,
                       successCount: IntParameter,
                       failureCount: IntParameter,
                       cancelCount: IntParameter): Unit = {
    val ste = StatusEvent(axisStatsEventPrefix).madd(axisName,
                                                     datumCount,
                                                     moveCount,
                                                     homeCount,
                                                     limitCount,
                                                     successCount,
                                                     failureCount,
                                                     cancelCount)
//    log.debug(s"Axis stats publish of $axisStatsEventPrefix: $ste")
//    telemetryService.foreach(_.publish(ste).onComplete {
//      case Failure(ex) => log.error(s"TrombonePublisher failed to publish trombone axis stats: $ste", ex)
//      case _           =>
//    })
  }

}

object TrombonePublisher {
  def make(assemblyContext: AssemblyContext, ctx: ActorContext[TromboneState]) =
    new TrombonePublisher(assemblyContext, ctx)

  sealed trait TrombonePublisherMsg

  case class TrombonePublisherMsgE(tromboneState: TromboneState) extends TrombonePublisherMsg

  /**
   * Used by actors wishing to cause an event for AO ESW
   * @param naElevation elevation update
   * @param naRange range update
   */
  case class AOESWUpdate(naElevation: DoubleParameter, naRange: DoubleParameter) extends TrombonePublisherMsg

  /**
   * Used by actors wishing to cause an engineering event update
   * @param focusError focus error value as DoubleParameter
   * @param stagePosition stage position as a DoubleParameter
   * @param zenithAngle zenith angle update as a DoubleParameter
   */
  case class EngrUpdate(focusError: DoubleParameter, stagePosition: DoubleParameter, zenithAngle: DoubleParameter)
      extends TrombonePublisherMsg

  case class AxisStateUpdate(axisName: StringParameter,
                             position: IntParameter,
                             state: ChoiceParameter,
                             inLowLimit: BooleanParameter,
                             inHighLimit: BooleanParameter,
                             inHome: BooleanParameter)
      extends TrombonePublisherMsg

  case class AxisStatsUpdate(axisName: StringParameter,
                             initCount: IntParameter,
                             moveCount: IntParameter,
                             homeCount: IntParameter,
                             limitCount: IntParameter,
                             successCount: IntParameter,
                             failCount: IntParameter,
                             cancelCount: IntParameter)
      extends TrombonePublisherMsg

}
