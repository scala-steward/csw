package csw.services.logging.components.iris;

import akka.typed.javadsl.Actor;
import akka.typed.javadsl.ActorContext;
import csw.services.logging.javadsl.ILogger;

public class JIrisSupervisorTypedActor extends JIrisTypedActorLogger {

    private ILogger log = getLogger();

    public JIrisSupervisorTypedActor(ActorContext actorContext) {
        super(actorContext);
    }

    @Override
    public Actor.Receive createReceive() {
        return null;
        /*return receiveBuilder()
                .match(String.class, msg -> msg.equals("trace"), msg -> log.trace(() -> msg))
                .match(String.class, msg -> msg.equals("debug"), msg -> log.debug(() -> msg))
                .match(String.class, msg -> msg.equals("info"), msg -> log.info(() -> msg))
                .match(String.class, msg -> msg.equals("warn"), msg -> log.warn(() -> msg))
                .match(String.class, msg -> msg.equals("error"), msg -> log.error(() -> msg))
                .match(String.class, msg -> msg.equals("fatal"), msg -> log.fatal(() -> msg))
                .build();*/
    }
}
