package csw.params.core.formats

import java.lang
import java.time.Instant

import com.github.ghik.silencer.silent
import csw.params.core.generics.{KeyType, Parameter}
import play.api.libs.json._

/**
 * Derived Json formats to be used by play-json library. For scala, the library does not support character so a custom
 * implementation is provided. All the java datatype formats are mapped onto their corresponding scala datatypes.
 */
trait MiscJsonFormats {
  implicit def parameterFormat2: Format[Parameter[_]] = new Format[Parameter[_]] {
    override def writes(obj: Parameter[_]): JsValue = {
      val format = obj.keyType.paramFormat.asInstanceOf[Format[Parameter[Any]]]
      format.writes(obj.asInstanceOf[Parameter[Any]])
    }

    override def reads(json: JsValue): JsResult[Parameter[_]] = {
      val value = (json \ "keyType").as[KeyType[_]]
      value.paramFormat.reads(json)
    }
  }

  private def formatFactory[S: Format, J](implicit @silent conversion: S => J): Format[J] =
    implicitly[Format[S]].asInstanceOf[Format[J]]

  //scala
  implicit val charFormat: Format[Char] = new Format[Char] {
    override def reads(json: JsValue): JsResult[Char] = json match {
      case JsString(str) if str.length == 1 => JsSuccess(str.head)
      case _                                => JsError("error.expected.char")
    }

    override def writes(o: Char): JsValue = JsString(o.toString)
  }

  implicit def optionFormat[T: Format]: Format[Option[T]] = new Format[Option[T]] {
    override def reads(json: JsValue): JsResult[Option[T]] = json.validateOpt[T]

    override def writes(o: Option[T]): JsValue = o match {
      case Some(t) ⇒ implicitly[Writes[T]].writes(t)
      case None    ⇒ JsNull
    }
  }

  //java
  implicit val booleanFormat: Format[lang.Boolean]     = formatFactory[Boolean, java.lang.Boolean]
  implicit val characterFormat: Format[lang.Character] = formatFactory[Char, java.lang.Character]
  implicit val byteFormat: Format[lang.Byte]           = formatFactory[Byte, java.lang.Byte]
  implicit val shortFormat: Format[lang.Short]         = formatFactory[Short, java.lang.Short]
  implicit val longFormat: Format[lang.Long]           = formatFactory[Long, java.lang.Long]
  implicit val integerFormat: Format[lang.Integer]     = formatFactory[Int, java.lang.Integer]
  implicit val floatFormat: Format[lang.Float]         = formatFactory[Float, java.lang.Float]
  implicit val doubleFormat: Format[lang.Double]       = formatFactory[Double, java.lang.Double]
  implicit val timestampFormat: Format[Instant] = new Format[Instant] {
    override def reads(json: JsValue): JsResult[Instant] = JsSuccess(Instant.parse(json.as[String]))
    override def writes(instant: Instant): JsValue       = JsString(instant.toString)
  }
}
