package csw.param

import java.util.Optional

import csw.param.UnitsOfMeasure.{NoUnits, Units}

import scala.collection.JavaConverters.{iterableAsScalaIterableConverter, seqAsJavaListConverter}
import scala.collection.immutable.Vector
import scala.compat.java8.OptionConverters.RichOptionForJava8

/**
 * The type of a value for an BooleanKey
 *
 * @param keyName the name of the key
 * @param values  the value for the key
 * @param units   the units of the value
 */
final case class BooleanParameter(keyName: String, values: Vector[Boolean], units: Units) extends Parameter[Boolean] {

  override def withUnits(unitsIn: Units): BooleanParameter = copy(units = unitsIn)

  //TODO: Proposal 2
  def jValues: java.util.List[java.lang.Boolean] = values.map(i => i: java.lang.Boolean).asJava

  def jGet(index: Int): Optional[java.lang.Boolean] = get(index).asJava.map(i ⇒ i: Boolean)
}

/**
 * A key of Boolean values
 *
 * @param nameIn the name of the key
 */
final case class BooleanKey(nameIn: String) extends Key[Boolean, BooleanParameter](nameIn) {

  override def set(v: Vector[Boolean], units: Units = NoUnits) = BooleanParameter(keyName, v, units)

  override def set(v: Boolean*) = BooleanParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)

  //TODO: Proposal 2
  def jSet(v: java.util.List[java.lang.Boolean], units: Units): BooleanParameter =
    set(v.asScala.toVector.map(i => i: Boolean), units)

  def jSet(v: java.lang.Boolean*): BooleanParameter =
    jSet(v.toList.asJava, UnitsOfMeasure.NoUnits)
}
