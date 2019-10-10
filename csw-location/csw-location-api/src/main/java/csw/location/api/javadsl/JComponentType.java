package csw.location.api.javadsl;

import csw.location.models.ComponentType;

/**
 * Helper class for Java to get the handle of `ComponentType` which is fundamental to LocationService library
 */
@SuppressWarnings("unused")
public class JComponentType {

    /**
     * A container for components (assemblies and HCDs)
     */
    public static final ComponentType Container = ComponentType.Container$.MODULE$;

    /**
     * A component that controls a hardware device
     */
    public static final ComponentType HCD = ComponentType.HCD$.MODULE$;

    /**
     * A component that controls one or more HCDs or assemblies
     */
    public static final ComponentType Assembly = ComponentType.Assembly$.MODULE$;

    /**
     * A component that controls one or more sequencers or assemblies
     */
    public static final ComponentType Sequencer = ComponentType.Sequencer$.MODULE$;

    /**
     * A sequence component e.g ocs_1, iris_1
     */
    public static final ComponentType SequenceComponent = ComponentType.SequenceComponent$.MODULE$;

    /**
     * A general purpose service component (actor and/or web service application)
     */
    public static final ComponentType Service = ComponentType.Service$.MODULE$;
}
