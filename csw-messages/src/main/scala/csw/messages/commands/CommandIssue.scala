package csw.messages.commands

import ai.x.play.json.Jsonx
import play.api.libs.json.Format

/**
 * Describes a command issue with appropriate reason for validation failure
 */
sealed trait CommandIssue {

  /**
   * A method to access the reason of command issue
   *
   * @return the reason for a command issue
   */
  def reason: String
}

object CommandIssue {

  /**
   * Returned when a command is missing a required key/parameter
   *
   * @param reason describing the cause of this issue
   */
  final case class MissingKeyIssue(reason: String) extends CommandIssue

  /**
   * Returned when an Assembly receives a configuration with a Prefix that it doesn't support
   *
   * @param reason describing the cause of this issue
   */
  final case class WrongPrefixIssue(reason: String) extends CommandIssue

  /**
   * Returned when the parameter for a key is not the correct type (i.e. int vs double, etc.)
   *
   * @param reason describing the cause of this issue
   */
  final case class WrongParameterTypeIssue(reason: String) extends CommandIssue

  /**
   * Returned when a parameter value does not have the correct units
   *
   * @param reason describing the cause of this issue
   */
  final case class WrongUnitsIssue(reason: String) extends CommandIssue

  /**
   * Returned when a command does not have the correct number of parameters
   *
   * @param reason describing the cause of this issue
   */
  final case class WrongNumberOfParametersIssue(reason: String) extends CommandIssue

  //  // Returned when an Assembly receives a ConfigArg with multiple configs, but it can only execute one at a time
  //  final case class SingleConfigOnlyIssue(reason: String) extends ValidationIssue

  /**
   * Returned when an Assembly receives a command and one is already executing
   *
   * @param reason describing the cause of this issue
   */
  final case class AssemblyBusyIssue(reason: String) extends CommandIssue

  /**
   * Returned when some required location is not available
   *
   * @param reason describing the cause of this issue
   */
  final case class UnresolvedLocationsIssue(reason: String) extends CommandIssue

  /**
   *  Parameter of a command is out of range
   *
   * @param reason describing the cause of this issue
   */
  final case class ParameterValueOutOfRangeIssue(reason: String) extends CommandIssue

  /**
   * The component is in the wrong internal state to handle a command
   *
   * @param reason describing the cause of this issue
   */
  final case class WrongInternalStateIssue(reason: String) extends CommandIssue

  /**
   * A command is unsupported in the current state
   *
   * @param reason describing the cause of this issue
   */
  final case class UnsupportedCommandInStateIssue(reason: String) extends CommandIssue

  /**
   * A command is unsupported by component
   *
   * @param reason describing the cause of this issue
   */
  final case class UnsupportedCommandIssue(reason: String) extends CommandIssue

  /**
   * A required service is not available
   *
   * @param reason describing the cause of this issue
   */
  final case class RequiredServiceUnavailableIssue(reason: String) extends CommandIssue

  /**
   *  sA required HCD is not available
   *
   * @param reason describing the cause of this issue
   */
  final case class RequiredHCDUnavailableIssue(reason: String) extends CommandIssue

  /**
   * A required Assembly is not available
   *
   * @param reason describing the cause of this issue
   */
  final case class RequiredAssemblyUnavailableIssue(reason: String) extends CommandIssue

  /**
   * A required Sequencer is not available
   *
   * @param reason describing the cause of this issue
   */
  final case class RequiredSequencerUnavailableIssue(reason: String) extends CommandIssue

  /**
   * Returned when command received by one component when it is locked by other component
   *
   * @param reason describing the cause of this issue
   */
  final case class ComponentLockedIssue(reason: String) extends CommandIssue

  /**
   * Returned when some other issue occurred apart from those already defined
   *
   * @param reason describing the cause of this issue
   */
  final case class OtherIssue(reason: String) extends CommandIssue

  implicit lazy val jsonFormatMissingKeyIssue: Format[MissingKeyIssue]   = Jsonx.formatCaseClass[MissingKeyIssue]
  implicit lazy val jsonFormatWrongPrefixIssue: Format[WrongPrefixIssue] = Jsonx.formatCaseClass[WrongPrefixIssue]
  implicit lazy val jsonFormatWrongUnitsIssue: Format[WrongUnitsIssue]   = Jsonx.formatCaseClass[WrongUnitsIssue]
  implicit lazy val jsonFormatWrongNumberOfParametersIssue: Format[WrongNumberOfParametersIssue] =
    Jsonx.formatCaseClass[WrongNumberOfParametersIssue]
  implicit lazy val jsonFormatWrongParameterTypeIssue: Format[WrongParameterTypeIssue] =
    Jsonx.formatCaseClass[WrongParameterTypeIssue]
  implicit lazy val jsonFormatAssemblyBusyIssue: Format[AssemblyBusyIssue] = Jsonx.formatCaseClass[AssemblyBusyIssue]
  implicit lazy val jsonFormatUnresolvedLocationsIssue: Format[UnresolvedLocationsIssue] =
    Jsonx.formatCaseClass[UnresolvedLocationsIssue]
  implicit lazy val jsonFormatParameterValueOutOfRangeIssue: Format[ParameterValueOutOfRangeIssue] =
    Jsonx.formatCaseClass[ParameterValueOutOfRangeIssue]
  implicit lazy val jsonFormatWrongInternalStateIssue: Format[WrongInternalStateIssue] =
    Jsonx.formatCaseClass[WrongInternalStateIssue]
  implicit lazy val jsonFormatUnsupportedCommandInStateIssue: Format[UnsupportedCommandInStateIssue] =
    Jsonx.formatCaseClass[UnsupportedCommandInStateIssue]
  implicit lazy val jsonFormatUnsupportedCommandIssue: Format[UnsupportedCommandIssue] =
    Jsonx.formatCaseClass[UnsupportedCommandIssue]
  implicit lazy val jsonFormatRequiredServiceUnavailableIssue: Format[RequiredServiceUnavailableIssue] =
    Jsonx.formatCaseClass[RequiredServiceUnavailableIssue]
  implicit lazy val jsonFormatRequiredHCDUnavailableIssue: Format[RequiredHCDUnavailableIssue] =
    Jsonx.formatCaseClass[RequiredHCDUnavailableIssue]
  implicit lazy val jsonFormatRequiredAssemblyUnavailableIssue: Format[RequiredAssemblyUnavailableIssue] =
    Jsonx.formatCaseClass[RequiredAssemblyUnavailableIssue]
  implicit lazy val jsonFormatRequiredSequencerUnavailableIssue: Format[RequiredSequencerUnavailableIssue] =
    Jsonx.formatCaseClass[RequiredSequencerUnavailableIssue]
  implicit lazy val jsonFormatComponentLockedIssue: Format[ComponentLockedIssue] = Jsonx.formatCaseClass[ComponentLockedIssue]
  implicit lazy val jsonFormatOtherIssue: Format[OtherIssue]                     = Jsonx.formatCaseClass[OtherIssue]
  implicit lazy val jsonFormat: Format[CommandIssue]                             = Jsonx.formatSealed[CommandIssue]
}
