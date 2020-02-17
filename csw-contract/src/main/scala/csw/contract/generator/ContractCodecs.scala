package csw.contract.generator

import io.bullet.borer.Encoder
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveEncoder

object ContractCodecs extends ContractCodecs
trait ContractCodecs {
  implicit lazy val endpointCodec: Encoder[Endpoint]      = deriveEncoder
  implicit lazy val modelTypeCodec: Encoder[ModelType[_]] = Encoder((w, v) => v.write(w))

  implicit lazy val modelSetCodec: Encoder[ModelSet] = Encoder[Map[String, ModelType[_]]]
    .contramap(_.modelTypes.map(x => x.name -> x).toMap)

  implicit lazy val contractCodec: Encoder[Contract] = deriveEncoder
  implicit lazy val serviceCodec: Encoder[Service]   = deriveEncoder
  implicit lazy val servicesCodec: Encoder[Services] = deriveEncoder
}
