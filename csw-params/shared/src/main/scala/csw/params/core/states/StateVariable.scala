package csw.params.core.states

import csw.params.commands.{Nameable, Setup}
import csw.params.core.generics.{Parameter, ParameterSetKeyData, ParameterSetType}
import csw.prefix.models.Prefix
import csw.serializable.CommandSerializable

import scala.annotation.varargs
import scala.jdk.CollectionConverters._

/**
 * Base trait for state variables
 */
sealed trait StateVariable extends CommandSerializable {

  /**
   * identifies the target subsystem
   */
  val prefix: Prefix

  /**
   * an optional initial set of items (keys with values)
   */
  val paramSet: Set[Parameter[_]]

  /**
   * identifies the name of the state
   */
  val stateName: StateName

  /**
   * A name identifying the type of command, such as "setup", "observe".
   * This is used in the JSON and toString output.
   */
  def typeName: String

  /**
   * A common toString method for all concrete implementation
   *
   * @return the string representation of command
   */
  override def toString: String =
    s"$typeName(source=$prefix, stateName=$stateName, paramSet=$paramSet)"
}
object StateVariable {

  /**
   * Type of a function that returns true if two state variables (demand and current)
   * match (or are close enough, which is implementation dependent)
   */
  type Matcher = (DemandState, CurrentState) => Boolean

  /**
   * The default matcher for state variables tests for an exact match
   *
   * @param demand the demand state
   * @param current the current state
   * @return true if the demand and current states match (in this case, are equal)
   */
  def defaultMatcher(demand: DemandState, current: CurrentState): Boolean =
    demand.stateName == current.stateName && demand.prefixStr == current.prefixStr && demand.paramSet == current.paramSet

  /**
   * A Java helper method to create CurrentState
   *
   * @param states one or more CurrentState objects
   * @return a new CurrentStates object containing all the given CurrentState objects
   */
  @varargs
  def createCurrentStates(states: CurrentState*): CurrentStates = CurrentStates(states.toList)

  /**
   * A Java helper method to create CurrentState
   *
   * @param states one or more CurrentState objects
   * @return a new CurrentStates object containing all the given CurrentState objects
   */
  def createCurrentStates(states: java.util.List[CurrentState]): CurrentStates = CurrentStates(states.asScala.toList)
}

/**
 * A state variable that indicates the ''demand'' or requested state.
 *
 * @param prefix identifies the target subsystem
 * @param stateName identifies the name of the state
 * @param paramSet initial set of items (keys with values)
 */
case class DemandState(prefix: Prefix, stateName: StateName, paramSet: Set[Parameter[_]])
    extends ParameterSetType[DemandState]
    with ParameterSetKeyData
    with StateVariable {

  /**
   * A Java helper method to construct with String
   */
  def this(prefix: Prefix, stateName: StateName) = this(prefix, stateName, Set.empty[Parameter[_]])

  /**
   * A Java helper method to create a DemandState from a Setup
   */
  def this(stateName: StateName, command: Setup) = this(command.source, stateName, command.paramSet)

  /**
   * Create a new DemandState instance when a parameter is added or removed
   *
   * @param data set of parameters
   * @return a new instance of DemandState with provided data
   */
  override protected def create(data: Set[Parameter[_]]): DemandState = copy(paramSet = data)
}

object DemandState {

  /**
   * A helper method to create DemandState
   *
   * @param prefix identifies the target subsystem
   * @param stateName identifies the name of the state
   * @param paramSet an optional initial set of items (keys with values)
   * @return an instance of DemandState
   */
  def apply(prefix: Prefix, stateName: StateName, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): DemandState =
    new DemandState(prefix, stateName).madd(paramSet)
}

/**
 * A state variable that indicates the ''current'' or actual state.
 *
 * @param prefix       identifies the target subsystem
 * @param stateName identifies the name of the state
 * @param paramSet     an optional initial set of items (keys with values)
 */
case class CurrentState(
    prefix: Prefix,
    stateName: StateName,
    paramSet: Set[Parameter[_]]
) extends ParameterSetType[CurrentState]
    with ParameterSetKeyData
    with StateVariable {

  /**
   * A Java helper method to construct with String
   */
  def this(prefix: Prefix, currentStateName: StateName) = this(prefix, currentStateName, Set.empty[Parameter[_]])

  /**
   * A Java helper method to create a CurrentState from a Setup
   */
  def this(currentStateName: StateName, command: Setup) = this(command.source, currentStateName, command.paramSet)

  /**
   * Create a new CurrentState instance when a parameter is added or removed
   *
   * @param data set of parameters
   * @return a new instance of CurrentState with provided data
   */
  override protected def create(data: Set[Parameter[_]]): CurrentState = copy(paramSet = data)
}

object CurrentState {

  /**
   * A helper method to create CurrentState
   *
   * @param prefix identifies the target subsystem
   * @param stateName identifies the name of the state
   * @param paramSet an optional initial set of items (keys with values)
   * @return an instance of CurrentState
   */
  def apply(
      prefix: Prefix,
      stateName: StateName,
      paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]
  ): CurrentState = new CurrentState(prefix, stateName).madd(paramSet)

  implicit object NameableCurrentState extends Nameable[CurrentState] {
    override def name(state: CurrentState): StateName = state.stateName
  }
}
