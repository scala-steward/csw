package csw.location.client.internal

import java.io.IOException

import akka.actor.{ActorSystem, CoordinatedShutdown, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream._
import akka.{Done, NotUsed}
import csw.location.api.exceptions.{OtherLocationIsRegistered, RegistrationFailed}
import csw.location.api.formats.LocationJsonSupport
import csw.location.api.models.{Registration, RegistrationResult, _}
import csw.location.api.scaladsl.LocationService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.Json

import scala.async.Async.{async, await}
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Random, Success, Try}

private[csw] class LocationServiceClient(serverIp: String, serverPort: Int)(implicit val actorSystem: ActorSystem,
                                                                            mat: Materializer)
    extends LocationService
    with PlayJsonSupport
    with LocationJsonSupport { outer ⇒

  import actorSystem.dispatcher
  implicit val scheduler: Scheduler = actorSystem.scheduler

  private val baseUri = s"http://$serverIp:$serverPort/location"

  val hostPoolClientFlow
    : Flow[(HttpRequest, Promise[HttpResponse]), (Try[HttpResponse], Promise[HttpResponse]), Http.HostConnectionPool] =
    Http().cachedHostConnectionPool[Promise[HttpResponse]](serverIp, serverPort)

  val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    Http().outgoingConnection(serverIp, serverPort)

  lazy val sourceHostConnection = Source
    .queue(1024, OverflowStrategy.backpressure)
    .via(hostPoolClientFlow)
    .runForeach {
      case (Success(r), _) ⇒
    }

  val queue =
    Source
      .queue[(HttpRequest, Promise[HttpResponse])](1024, OverflowStrategy.backpressure)
      .via(hostPoolClientFlow)
      .toMat(Sink.foreach({
        case ((Success(resp), p)) => p.success(resp)
        case ((Failure(e), p))    => p.failure(e)
      }))(Keep.left)
      .run()

  def queueRequest(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap {
      case QueueOfferResult.Enqueued    => responsePromise.future
      case QueueOfferResult.Dropped     => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed =>
        Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }

  override def register(registration: Registration): Future[RegistrationResult] = async {
    val uri           = Uri(baseUri + "/register")
    val requestEntity = await(Marshal(registration).to[RequestEntity])
    val request       = HttpRequest(HttpMethods.POST, uri = uri, entity = requestEntity)
    val response      = await(Http().singleRequest(request))

    response.status match {
      case StatusCodes.OK ⇒
        val location0 = await(Unmarshal(response.entity).to[Location])
        CoordinatedShutdown(actorSystem).addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "unregister")(
          () ⇒ unregister(location0.connection)
        )
        new RegistrationResult {
          override def unregister(): Future[Done] = outer.unregister(location0.connection)
          override def location: Location         = location0
        }
      case x @ StatusCodes.BadRequest          ⇒ throw OtherLocationIsRegistered(x.reason)
      case x @ StatusCodes.InternalServerError ⇒ throw RegistrationFailed(x.reason)
      case _                                   ⇒ await(throwExOnInvalidResponse[RegistrationResult](request, response))
    }
  }

  override def unregister(connection: Connection): Future[Done] = async {
    val uri           = Uri(baseUri + "/unregister")
    val requestEntity = await(Marshal(connection).to[RequestEntity])
    val request       = HttpRequest(HttpMethods.POST, uri = uri, entity = requestEntity)
    val response      = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[Done])
  }

  override def unregisterAll(): Future[Done] = async {
    val uri      = Uri(baseUri + "/unregisterAll")
    val request  = HttpRequest(HttpMethods.POST, uri = uri)
    val response = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[Done])
  }

  override def find[L <: Location](connection: TypedConnection[L]): Future[Option[L]] = async {
    val uri      = Uri(s"$baseUri/find/${connection.name}")
    val request  = HttpRequest(HttpMethods.GET, uri = uri)
    val response = await(Http().singleRequest(request))
    response.status match {
      case StatusCodes.OK       ⇒ Some(await(Unmarshal(response.entity).to[Location]).asInstanceOf[L])
      case StatusCodes.NotFound ⇒ None
      case _                    ⇒ await(throwExOnInvalidResponse[Option[L]](request, response))
    }
  }

  override def resolve[L <: Location](connection: TypedConnection[L], within: FiniteDuration): Future[Option[L]] = async {
    val uri = Uri(
      s"$baseUri/resolve/${connection.name}?within=${within.length.toString + within.unit.toString.toLowerCase}"
    )
    val request  = HttpRequest(HttpMethods.GET, uri = uri)
    val response = await(Http().singleRequest(request))
    response.status match {
      case StatusCodes.OK       ⇒ Some(await(Unmarshal(response.entity).to[Location]).asInstanceOf[L])
      case StatusCodes.NotFound ⇒ None
      case _                    ⇒ await(throwExOnInvalidResponse[Option[L]](request, response))
    }
  }

  override def list: Future[List[Location]] = async {
    val uri      = Uri(baseUri + "/list")
    val request  = HttpRequest(HttpMethods.GET, uri = uri)
    val response = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[List[Location]])
  }

  def list1: Future[List[Location]] = async {
    val uri     = Uri(baseUri + "/list")
    val request = HttpRequest(HttpMethods.GET, uri = uri)

    val response = await(queueRequest(request))
    await(Unmarshal(response.entity).to[List[Location]])
  }

  override def list(componentType: ComponentType): Future[List[Location]] = async {
    val uri      = Uri(s"$baseUri/list?componentType=$componentType")
    val request  = HttpRequest(HttpMethods.GET, uri = uri)
    val response = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[List[Location]])
  }

  override def list(hostname: String): Future[List[Location]] = async {
    val uri      = Uri(s"$baseUri/list?hostname=$hostname")
    val request  = HttpRequest(HttpMethods.GET, uri = uri)
    val response = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[List[Location]])
  }

  override def list(connectionType: ConnectionType): Future[List[Location]] = async {
    val uri      = Uri(s"$baseUri/list?connectionType=${connectionType.entryName}")
    val request  = HttpRequest(HttpMethods.GET, uri = uri)
    val response = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[List[Location]])
  }

  override def listByPrefix(prefix: String): Future[List[AkkaLocation]] = async {
    val uri      = Uri(s"$baseUri/list?prefix=$prefix")
    val request  = HttpRequest(HttpMethods.GET, uri = uri)
    val response = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[List[AkkaLocation]])
  }

  override def track(connection: Connection): Source[TrackingEvent, KillSwitch] = {
    val uri     = Uri(s"$baseUri/track/${connection.name}")
    val request = HttpRequest(HttpMethods.GET, uri = uri)
    val sseStreamFuture = async {
      val response = await(Http().singleRequest(request))
      await(Unmarshal(response.entity).to[Source[ServerSentEvent, NotUsed]])
    }
    val sseStream = Source.fromFuture(sseStreamFuture).flatMapConcat(identity)
    sseStream.map(x ⇒ Json.parse(x.data).as[TrackingEvent]).viaMat(KillSwitches.single)(Keep.right)
  }

  def track3(connection: Connection): Source[TrackingEvent, KillSwitch] = {
    val uri     = Uri(s"$baseUri/track/${connection.name}")
    val request = HttpRequest(HttpMethods.GET, uri = uri)

    val eventualTuple = Source
      .single(request)
      .via(connectionFlow)
      .flatMapConcat { response ⇒
        val sseStreamFuture: Future[Source[ServerSentEvent, NotUsed]] =
          Unmarshal(response.entity).to[Source[ServerSentEvent, NotUsed]]
        val sseStream = Source.fromFuture(sseStreamFuture).flatMapConcat(identity)
        sseStream
      }

    eventualTuple.map(x ⇒ Json.parse(x.data).as[TrackingEvent]).viaMat(KillSwitches.single)(Keep.right)
  }

  override def subscribe(connection: Connection, callback: TrackingEvent ⇒ Unit): KillSwitch =
    track(connection).to(Sink.foreach(callback)).run()

  private def throwExOnInvalidResponse[T](request: HttpRequest, response: HttpResponse): Future[T] =
    Future.failed(
      new IOException(s"""Request failed with response status: [${response.status}]
           |Requested URI: [${request.uri}] and
           |Response body: ${response.entity}""".stripMargin)
    )
}
