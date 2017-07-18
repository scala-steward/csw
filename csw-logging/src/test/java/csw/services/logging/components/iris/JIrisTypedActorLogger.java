package csw.services.logging.components.iris;

import akka.typed.javadsl.ActorContext;
import csw.services.logging.javadsl.JComponentLoggerTypedActor;

abstract public class JIrisTypedActorLogger extends JComponentLoggerTypedActor {

    public static String NAME = "jIRIS";

    public JIrisTypedActorLogger(ActorContext actorContext) {
        super(actorContext);
    }

    @Override
    public String componentName() {
        return NAME;
    }
}