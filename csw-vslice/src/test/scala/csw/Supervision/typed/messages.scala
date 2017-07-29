package csw.Supervision.typed

import akka.typed.ActorRef

sealed trait ToParentMsg
case class Spawn(replyTo: ActorRef[Spawned]) extends ToParentMsg
case object AreYouThere                      extends ToParentMsg

sealed trait ToChildMsg
case class UpdateState(x: Int)                       extends ToChildMsg
case class GetState(replyTo: ActorRef[FromActorMsg]) extends ToChildMsg
case class Stop(ex: Exception)                       extends ToChildMsg

sealed trait FromActorMsg
case class Spawned(ref: ActorRef[ToChildMsg]) extends FromActorMsg
case class CurrentState(x: Int)               extends FromActorMsg
case class Stopped(message: String)           extends FromActorMsg
