//package csw.param.javadsl
//
//import java.util
//
//import csw.param.UnitsOfMeasure.Units
//import csw.param.{Key, Parameter, UnitsOfMeasure}
//
//import scala.collection.JavaConverters._
//
//final case class JBooleanParameter(keyName: String, values: java.util.Vector[Boolean], units: Units) extends Parameter[Boolean] {
//
//  override def withUnits(unitsIn: Units): JBooleanParameter = copy(units = unitsIn)
//}
//
//final case class JBooleanKey(nameIn: String) extends Key[Boolean, JBooleanParameter](nameIn) {
//
//  override def set(v: Boolean*) = {
//    val booleanses = util.Arrays.asList(v)
//    val booleans = new util.Vector[Boolean](v.toList.asJava)
//
//
//    JBooleanParameter(keyName, booleans, units = UnitsOfMeasure.NoUnits)
//  }
//
//  override def set(v: util.Vector[Boolean], units: Units): JBooleanParameter = JBooleanParameter(keyName, v, units)
//
//  override def set(v: Vector[Boolean], units: Units): JBooleanParameter = ???
//
//}
//
////
