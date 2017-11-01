package csw.messages.params.models

import java.util.UUID

import play.api.libs.json._

/**
 * Implementation of unique id for each running command (returned from a queue submit).
 */
object RunId {

  implicit val format: Format[RunId] = new Format[RunId] {
    override def writes(obj: RunId): JsValue           = JsString(obj.id)
    override def reads(json: JsValue): JsResult[RunId] = JsSuccess(RunId(json.as[String]))
  }

  def apply(): RunId  = new RunId(UUID.randomUUID().toString)
  def create(): RunId = new RunId(UUID.randomUUID().toString)

  def apply(uuid: UUID): RunId = new RunId(uuid.toString)
}

case class RunId(id: String)
