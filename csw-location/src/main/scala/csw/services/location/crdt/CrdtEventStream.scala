package csw.services.location.crdt

import javax.jmdns.JmDNS

import akka.actor.{Actor, Props}
import akka.cluster.ddata.LWWRegisterKey
import akka.cluster.ddata.Replicator.{Changed, DataDeleted}
import akka.stream.KillSwitch
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}
import csw.services.location.common.SourceExtensions
import csw.services.location.common.SourceExtensions.RichSource
import csw.services.location.scaladsl.ActorRuntime
import csw.services.location.scaladsl.models.Connection

class CrdtEventStream(jmDns: JmDNS, actorRuntime: ActorRuntime) {

  import actorRuntime._

  private val (source, queueF) = SourceExtensions.coupling[TrackingEvent]

  queueF.foreach(queue => actorSystem.actorOf(CrdtTrackingActor.props(queue)))

  val broadcast: Source[TrackingEvent, KillSwitch] = source.broadcast()
}

class CrdtTrackingActor(queue: SourceQueueWithComplete[TrackingEvent]) extends Actor {
  override def receive: Receive = {
    case c@Changed(key: LWWRegisterKey[ServiceLocation])   => queue.offer(Updated(c.get(key).value))
    case DataDeleted(key: LWWRegisterKey[ServiceLocation]) => queue.offer(Deleted(Connection.parse(key.id).get))
  }
}

object CrdtTrackingActor {
  def props(queue: SourceQueueWithComplete[TrackingEvent]): Props = Props(new CrdtTrackingActor(queue))
}
