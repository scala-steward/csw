package csw.param.proposal3

import java.time.Instant

import csw.param.Events._
import csw.param.UnitsOfMeasure.Units
import spray.json._

/**
 * Supports conversion of commands and events to/from JSON
 */
//noinspection TypeAnnotation
object ParameterSetJson extends DefaultJsonProtocol {
  implicit val unitsFormat = jsonFormat1(Units.apply)

  def jsd[P1: JsonFormat, T](construct: (P1) => T, f2: T ⇒ P1): JsonFormat[T] = new JsonFormat[T] {
    val value = implicitly[JsonFormat[P1]]
    override def read(json: JsValue): T = {
      construct(value.read(json))
    }

    override def write(obj: T): JsValue = {
      value.write(f2(obj))
    }
  }

  implicit val BoolRead = jsd[Boolean, java.lang.Boolean](x ⇒ x: java.lang.Boolean, x ⇒ x: Boolean)

  // JSON formats

  implicit val booleanParameterFormat = jsonFormat3(BooleanParameter.apply)

  implicit def eventTimeFormat: JsonFormat[EventTime] = new JsonFormat[EventTime] {
    def write(et: EventTime): JsValue = JsString(et.toString)

    def read(json: JsValue): EventTime = json match {
      case JsString(s) => Instant.parse(s)
      case _           => unexpectedJsValueError(json)
    }
  }

  // JSON type tags

  private val booleanType = classOf[BooleanParameter].getSimpleName

  private def unexpectedJsValueError(x: JsValue) = deserializationError(s"Unexpected JsValue: $x")

  // XXX TODO Use JNumber?
  def writeParameter[S <: AnyRef, I /*, J */ ](parameter: Parameter[S /*, J */ ]): JsValue = {
    val result: (JsString, JsValue) = parameter match {
      case i: BooleanParameter => (JsString(booleanType), booleanParameterFormat.write(i))
    }
    JsObject("type" -> result._1, "parameter" -> result._2)
  }

  def readParameterAndType(json: JsValue): Parameter[_ /*, _ */ ] = json match {
    case JsObject(fields) =>
      (fields("type"), fields("parameter")) match {
        case (JsString(`booleanType`), parameter) => booleanParameterFormat.read(parameter)
        case _                                    => unexpectedJsValueError(json)
      }
    case _ => unexpectedJsValueError(json)
  }
}
