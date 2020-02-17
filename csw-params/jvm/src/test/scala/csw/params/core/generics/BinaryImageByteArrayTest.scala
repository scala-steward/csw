package csw.params.core.generics

import java.nio.file.Files

import csw.commons.ResourceReader
import csw.params.core.generics.KeyType.ByteArrayKey
import csw.params.core.models.Units.encoder
import csw.params.core.models._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class BinaryImageByteArrayTest extends AnyFunSpec with Matchers {

  // DEOPSCSW-186: Binary value payload
  // DEOPSCSW-331: Complex payload - Include byte in paramset for Event and ObserveEvent
  describe("test ByteArrayKey") {
    it("should able to create parameter representing binary image") {
      val keyName                        = "imageKey"
      val imageKey: Key[ArrayData[Byte]] = ByteArrayKey.make(keyName)

      val imgPath  = ResourceReader.copyToTmp("/smallBinary.bin", ".bin")
      val imgBytes = Files.readAllBytes(imgPath)

      val binaryImgData: ArrayData[Byte]          = ArrayData.fromArray(imgBytes)
      val binaryParam: Parameter[ArrayData[Byte]] = imageKey -> binaryImgData withUnits encoder

      binaryParam.head shouldBe binaryImgData
      binaryParam.value(0) shouldBe binaryImgData
      binaryParam.units shouldBe encoder
      binaryParam.keyName shouldBe keyName
      binaryParam.size shouldBe 1
      binaryParam.keyType shouldBe KeyType.ByteArrayKey
    }
  }
}
