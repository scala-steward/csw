package csw.params.core.generics

import csw.params.core.models.{Choice, Choices, Units}

/**
 * A key for a choice item similar to an enumeration
 *
 * @param name  the name of the key
 * @param keyType reference to an object of type KeyType[Choice]
 * @param choices the available choices, the values set must be in the choices
 */
class GChoiceKey(name: String, keyType: KeyType[Choice], units: Units, val choices: Choices)
    extends Key[Choice](name, keyType, units) {

  /**
   * validates the input Seq of choices
   *
   * @param values one or more values
   */
  private def validate(values: Seq[Choice]): Unit =
    assert(values.forall(choices.contains), s"Bad choice for key: $keyName which must be one of: $choices")

  /**
   * Sets the values for the key using a variable number of arguments
   *
   * @param values one or more values
   * @return a parameter containing the key name, values
   */
  override def set(value: Choice, values: Choice*): Parameter[Choice] = {
    validate(value +: values.toList)
    super.set(value, values: _*)
  }
}
