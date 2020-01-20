# Event Service

The Event Service implements the [publish/subscribe messaging paradigm](https://en.wikipedia.org/wiki/Publish%E2%80%93subscribe_pattern) where 
one component publishes an event and all clients that have subscribed receive the event. In CSW, the events published are
described under the `messages` documentation @ref:[here](./../params/events.md).
One advantage of this type of message system for the Event Service is that publishers and subscribers are decoupled. 
This decoupling of publishers and subscribers can allow for greater scalability and a more dynamic network topology.
Publishers can publish regardless of whether there are subscribers, and subscribers can subscribe even if there are no publishers. 
The relationship between publishers and subscribers can be one-to-one, one-to-many, many to one, or even many-to-many. 

The Event Service is optimized for the high performance requirements of events as demands with varying rates, (e.g. 100 Hz, 50 Hz etc.), but
can also be used with events that are published infrequently or when values change. 
In the TMT control system, events may be created as the output of a calculation by one component for the input to a calculation in 
one or more other components. Demand events often consist of events that are published at a specific rate.

The Event Service also stores the most recent published event for every unique event by prefix and name. This allows components 
needing to check the state of another component can do so without the overhead of subscribing. 

## Dependencies

If you already have a dependency on `csw-framework` in your `build.sbt`, then you can skip this as `csw-framework` depends on `csw-event-client`
Otherwise add below dependency in your `build.sbt`

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-event-client" % "$version$"
    ```
    @@@

## Accessing Event Service

When you create component handlers using `csw-framework` as explained @ref:[here](./../framework/creating-components.md), 
you get a handle to `EventService` which is created by `csw-framework`, which provides the following features: 

__Access to `defaultPublisher`__: Using the `defaultPublisher` in `EventService`, you can publish a single event or a stream 
of demand events to Event Service. 
In most cases, you should use the `defaultPublisher`, because you can then pass the instance of `EventService` in worker actors 
or different places in your code and call `eventService.defaultPublisher` to access the publisher. 
Each `EventPublisher` has its own TCP connection to the Event Service.
When you reuse the `defaultPublisher` instance of `EventPublisher`, all events published go through same TCP connection. 

__Access to `defaultSubscriber`__: Using the `defaultSubscriber`, you can subscribe to specific event keys. 
You can share the `defaultSubscriber` in the same way as `defaultPublisher` by passing an instance of `EventService` to different parts of your code.
Unlike `defaultPublisher`, each subscription with `defaultSubscriber.subscribe` creates a new TCP connection for just that subscription. 
This behavior is the same whether you use the `defaultSubscriber` or the `makeNewSubscriber` call on `EventService`.

Each `EventSubscriber` also has one TCP connection that is used to provide the latest event from the event server when
subscribing and for the explicit `get` calls.  That means, with `defaultSubscriber`, you are sharing same connection 
for getting latest events and creating a new connection for each subscribe call.  The underlying event server can handle
many connections, but it is good to understand how connections are used and reused.

__Creating a new `Publisher` or `Subscriber`__:
The `makeNewPublisher` API of Event Service can be used to create a new publisher which would internally create a new TCP connection to the Event Store.
One of the use cases of this API could be to publish high frequency event streams in order to dedicate a separate connection to 
demanding streams without affecting the performance of all other low frequency (for ex. 1Hz, 20Hz etc.) event streams.

However, `makeNewSubscriber` API does not really have any specific use cases. Both `defaultSubscriber` and `makeNewSubscriber` 
APIs behave almost similar since the `subscribe` API of EventService itself creates a new connection for every subscription. 
Prefer using `defaultSubscriber` over `makeNewSubscriber`.

## Usage of EventPublisher

Below examples demonstrate the usage of multiple variations of the publish API.

### For Single Event

This is the simplest API to publish a single event. It returns a Future which will complete successfully if the event is published or 
fail immediately with a @scaladoc[PublishFailure](csw/event/api/exceptions/PublishFailure) exception if the component cannot
publish the event.

Scala
:   @@snip [EventPublishExamples.scala](../../../../examples/src/main/scala/example/event/EventPublishExamples.scala) { #single-event }

Java
:   @@snip [JEventPublishExamples.java](../../../../examples/src/main/java/example/event/JEventPublishExamples.java) { #single-event }

### With Generator

A generator is useful when component code needs to publish events with a specific frequency.
The following example demonstrates the usage of publish API with event generator which will publish one event 
at each `interval`. `eventGenerator` is a function responsible for generating events. It can hold domain specific logic 
of generating new events based on certain conditions.

Scala
:   @@snip [EventPublishExamples.scala](../../../../examples/src/main/scala/example/event/EventPublishExamples.scala) { #event-generator }

Java
:   @@snip [JEventPublishExamples.java](../../../../examples/src/main/java/example/event/JEventPublishExamples.java) { #event-generator }

@@@ note
Callbacks like `eventGenerator` are not thread-safe on the JVM. If you are doing side effects/mutations inside the callback, you should ensure that it is done in a thread-safe way inside an actor. Here is an [example]($github.base_url$/examples/src/main/scala/example/event/ConcurrencyInCallbacksExample.scala) of how it can be done.
@@@


### With Event Stream

In order to publish a continuous stream of events, this stream-based API can also be used. 
If an infinite stream is provided, shutdown of the stream needs to be taken care by the users.
(Note that streams discussed here are an Akka feature that is supported in event publisher and subscriber APIs. 
See [Akka stream documentation.](https://doc.akka.io/docs/akka/current/stream/index.html?language=scala))

Scala
:   @@snip [EventPublishExamples.scala](../../../../examples/src/main/scala/example/event/EventPublishExamples.scala) { #with-source }

Java
:   @@snip [JEventPublishExamples.java](../../../../examples/src/main/java/example/event/JEventPublishExamples.java) { #with-source }

This API also demonstrates the usage of an onError callback which can be used to handle events that failed while being published. 

You can find complete list of APIs supported by `EventPublisher` and `IEventPublisher` with detailed description of each API here: 

* @scaladoc[EventPublisher](csw/event/api/scaladsl/EventPublisher)
* @javadoc[IEventPublisher](csw/event/api/javadsl/IEventPublisher)

## Usage of EventSubscriber

The EventSubscriber API has several options available that are useful in different situations.
Examples below demonstrate the usage of multiple variations available in the subscribe API.

### With Callback

The example shown below takes a set of event keys to subscribe to and a callback function which will be called on each 
event received by the event stream. This is the simplest and most commonly used API. The example below uses an inline
function, but that is not necessary. 

Scala
:   @@snip [EventSubscribeExamples.scala](../../../../examples/src/main/scala/example/event/EventSubscribeExamples.scala) { #with-callback }

Java
:   @@snip [JEventSubscribeExamples.java](../../../../examples/src/main/java/example/event/JEventSubscribeExamples.java) { #with-callback }


@@@ note
Callbacks are not thread-safe on the JVM. If you need to do side effects/mutations, prefer using `subscribeActorRef` API.
@@@


### With Asynchronous Callback

This API is useful when you want to subscribe to events with a callback that has an asynchronous behavior. The callback is 
of type `Event => Future` and it ensures that the event callbacks are called sequentially in such a way that the subsequent 
execution will start only after the prior one finishes. 
This API gives the guarantee of ordered execution of the asynchronous callbacks.

Scala
:   @@snip [EventSubscribeExamples.scala](../../../../examples/src/main/scala/example/event/EventSubscribeExamples.scala) { #with-async-callback }

Java
:   @@snip [JEventSubscribeExamples.java](../../../../examples/src/main/java/example/event/JEventSubscribeExamples.java) { #with-async-callback }

### With ActorRef

If there is a need to mutate state on receiving each event, then it is recommended to use this API and send a message to an actor. 
To use this API, you have to create an actor which takes event and then you can safely keep mutable state inside this actor. 
In the example shown below, `eventHandler` is the actorRef which accepts events. 

Scala
:   @@snip [EventSubscribeExamples.scala](../../../../examples/src/main/scala/example/event/EventSubscribeExamples.scala) { #with-actor-ref }

Java
:   @@snip [JEventSubscribeExamples.java](../../../../examples/src/main/java/example/event/JEventSubscribeExamples.java) { #with-actor-ref }


### Receive Event Stream

This API takes a set of Event keys to subscribe to and returns a 
Source of events (see [Akka stream documentation](https://doc.akka.io/docs/akka/current/stream/index.html?language=scala)). 
This API gives more control to the user to customize the behavior of an event stream.

Scala
:   @@snip [EventSubscribeExamples.scala](../../../../examples/src/main/scala/example/event/EventSubscribeExamples.scala) { #with-source }

Java
:   @@snip [JEventSubscribeExamples.java](../../../../examples/src/main/java/example/event/JEventSubscribeExamples.java) { #with-source }

### Controlling Subscription Rate

In all the examples shown above, events are received by the subscriber as soon as they are published. 
There will be scenarios where you would like to control the rate of events received by your code. 
For instance, slow subscribers can receive events at their own specified speed rather than being overloaded 
with events to catch up with the publisher's speed. 

All the APIs in EventSubscriber can be provided with an `interval` and a `SubscriptionMode` 
to control the subscription rate. Following example demonstrates this with the subscribeCallback API. 

Scala
:   @@snip [EventSubscribeExamples.scala](../../../../examples/src/main/scala/example/event/EventSubscribeExamples.scala) { #with-subscription-mode }

Java
:   @@snip [JEventSubscribeExamples.java](../../../../examples/src/main/java/example/event/JEventSubscribeExamples.java) { #with-subscription-mode }
 

There are two types of Subscription modes:

* `RateAdapterMode` which ensures that an event is received exactly at each tick of the specified interval.
* `RateLimiterMode` which ensures that events are received as they are published along with the guarantee that 
no more than one event is delivered within a given interval.

Read more about Subscription Mode @scaladoc[here](csw/event/api/scaladsl/SubscriptionMode)

### Pattern Subscription

The following example demonstrates the usage of the pattern subscribe API with a callback. Events with keys that match the specified pattern 
and belong to the specified subsystem are received by the subscriber. 
The callback function provided is called on each event received.

Scala
:   @@snip [EventSubscribeExamples.scala](../../../../examples/src/main/scala/example/event/EventSubscribeExamples.scala) { #psubscribe }

Java
:   @@snip [JEventSubscribeExamples.java](../../../../examples/src/main/java/example/event/JEventSubscribeExamples.java) { #psubscribe }


@@@ warning
DO NOT include subsystem in the provided pattern. Final pattern generated will be provided pattern prepended with subsystem.
For Ex. `pSubscribe(Subsytem.WFOS, *)` will subscribe to event keys matching pattern : `wfos.*`
@@@ 

@@@ warning
The pattern-based subscribe API is provided because it is useful in testing, but *should not be used in production code*.
The use of certain patterns and many pattern-based subscriptions can impact the overall-performance of the Event Service.
@@@ 

### Event Subscription
On subscription to event keys, you receive an @scaladoc[EventSubscription](csw/event/api/scaladsl/EventSubscription) 
which provides following APIs:

* `unsubscribe`: Used to unsubscribe by destroying the event stream and releasing the the connection to the Event Server. 

* `ready`: check if event subscription is successful or not. 

You can find complete list of APIs supported by `EventSubscriber` and `IEventSubscriber` with detailed description 
of each API here: 

* @scaladoc[EventSubscriber](csw/event/api/scaladsl/EventSubscriber)
* @javadoc[IEventSubscriber](csw/event/api/javadsl/IEventSubscriber)

## Create Event Service
If you are not using csw-framework, you can create the @scaladoc[EventService](csw/event/api/scaladsl/EventService) 
using an @scaladoc[EventServiceFactory](csw/event/client/EventServiceFactory).

Scala
: @@snip [EventServiceCreationExamples.scala](../../../../examples/src/main/scala/example/event/EventServiceCreationExamples.scala) { #default-event-service }

Java
: @@snip [JEventServiceCreationExamples.java](../../../../examples/src/main/java/example/event/JEventServiceCreationExamples.java) { #default-event-service }

The provided implementation of Event Service is backed up by Redis. The above example demonstrates creation of Event Service 
with default Redis client options. 
You can optionally supply a RedisClient to the EventStore from outside which allows 
you to customize the behavior of the RedisClient used by Event Service, which will usually only be required in testing. 

RedisClient is an expensive resource. Reuse this instance as much as possible.

Note that it is the responsibility of the consumer of this API to shutdown the Redis Client when it is no longer in use.

Scala
:   @@snip [EventServiceCreationExamples.scala](../../../../examples/src/main/scala/example/event/EventServiceCreationExamples.scala) { #redis-event-service }

Java
:   @@snip [JEventServiceCreationExamples.java](../../../../examples/src/main/java/example/event/JEventServiceCreationExamples.java) { #redis-event-service }

## Technical Description
See @ref:[Event Service Technical Description](../technical/event/event.md).

## Source code for examples of creation

* [Scala Example]($github.base_url$/examples/src/main/scala/example/event/EventServiceCreationExamples.scala)
* [Java Example]($github.base_url$/examples/src/main/java/example/event/JEventServiceCreationExamples.java)

## Source code for examples of publishing

* [Scala Example]($github.base_url$/examples/src/main/scala/example/event/EventPublishExamples.scala)
* [Java Example]($github.base_url$/examples/src/main/java/example/event/JEventPublishExamples.java)

## Source code for examples of subscribing

* [Scala Example]($github.base_url$/examples/src/main/scala/example/event/EventSubscribeExamples.scala)
* [Java Example]($github.base_url$/examples/src/main/java/example/event/JEventSubscribeExamples.java)