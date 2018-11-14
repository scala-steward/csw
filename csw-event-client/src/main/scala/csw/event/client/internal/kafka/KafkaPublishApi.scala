package csw.event.client.internal.kafka

import akka.Done
import akka.kafka.ProducerSettings
import akka.stream.Materializer
import csw.event.api.exceptions.PublishFailure
import csw.event.client.internal.commons.PublishApi
import csw.event.client.pb.TypeMapperSupport
import csw.params.events.Event
import org.apache.kafka.clients.producer.{Callback, ProducerRecord}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

class KafkaPublishApi(producerSettings: Future[ProducerSettings[String, Array[Byte]]])(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends PublishApi {

  private val kafkaProducer = producerSettings.map(_.createKafkaProducer())

  override def publish(event: Event): Future[Done] = {
    val p = Promise[Done]
    kafkaProducer.map(_.send(eventToProducerRecord(event), completePromise(event, p))).recover {
      case NonFatal(ex) ⇒ p.failure(PublishFailure(event, ex))
    }
    p.future
  }

  override def shutdown(): Future[Done] = kafkaProducer.map { x =>
    scala.concurrent.blocking(x.close())
    Done
  }

  private def eventToProducerRecord(event: Event): ProducerRecord[String, Array[Byte]] =
    new ProducerRecord(event.eventKey.key, TypeMapperSupport.eventTypeMapper.toBase(event).toByteArray)

  // callback to be complete the future operation for publishing when the record has been acknowledged by the server
  private def completePromise(event: Event, promisedDone: Promise[Done]): Callback = {
    case (_, null)          ⇒ promisedDone.success(Done)
    case (_, ex: Exception) ⇒ promisedDone.failure(PublishFailure(event, ex))
  }
}
