package csw.param

sealed trait PublisherActorMessage

object PublisherActorMessage {

  /**
   * Subscribes the sender
   */
  case object Subscribe extends PublisherActorMessage

  /**
   * Unsubscribes the sender
   */
  case object Unsubscribe extends PublisherActorMessage

  // Message requesting the publisher to publish the current values
  case object RequestCurrent extends PublisherActorMessage

}
