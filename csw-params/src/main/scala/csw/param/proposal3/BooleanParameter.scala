package csw.param.proposal3

import java.{lang, util}

import csw.param.UnitsOfMeasure
import csw.param.UnitsOfMeasure.{NoUnits, Units}

import scala.annotation.varargs
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.immutable.Vector

/**
 * The type of a value for an BooleanKey
 *
 * @param keyName the name of the key
 * @param values  the value for the key
 * @param units   the units of the value
 */
final case class BooleanParameter(keyName: String, values: Vector[java.lang.Boolean], units: Units)
    extends Parameter[java.lang.Boolean] {

  override def withUnits(unitsIn: Units): BooleanParameter = copy(units = unitsIn)

}

/**
 * A key of Boolean values
 *
 * @param nameIn the name of the key
 */
final case class BooleanKey(nameIn: String) extends Key[java.lang.Boolean, BooleanParameter](nameIn) {

  override def set(v: Vector[java.lang.Boolean], units: Units = NoUnits) = BooleanParameter(keyName, v, units)

  @varargs
  override def set(v: java.lang.Boolean*) = BooleanParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)

  override def jSet(v: util.Vector[lang.Boolean], units: Units): BooleanParameter = set(v.asScala.toVector, units)
}

/**
 * A key of Boolean values
 *
 * @param nameIn the name of the key
 */
final case class JBooleanKey(nameIn: String) extends JKey[java.lang.Boolean, BooleanParameter](nameIn) {

  @varargs
  override def jSet(v: java.lang.Boolean*) = BooleanParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)

  override def jSet(v: util.Vector[lang.Boolean], units: Units): BooleanParameter = set(v.asScala.toVector, units)
}
