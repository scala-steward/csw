package csw.param.wip

import csw.param.{BooleanKey, BooleanParameter, UnitsOfMeasure}
import org.scalatest.{FunSuite, Matchers}

class ParameterTest extends FunSuite with Matchers {

  test("ddd") {
    val encoder                = "encoder"
    val encoderKey: BooleanKey = BooleanKey(encoder)

    val p1: BooleanParameter = encoderKey.set(true, false)
    val p2                   = encoderKey.set(Vector(true, false), UnitsOfMeasure.NoUnits)
    val p6                   = encoderKey.set(true, false).withUnits(UnitsOfMeasure.NoUnits)
    val p3                   = encoderKey -> true
    val p4                   = encoderKey -> Vector(true, false)
    val p5                   = encoderKey -> ((true, UnitsOfMeasure.NoUnits))

    List(p1, p2, p4, p6).foreach { parameter ⇒
      parameter.keyName shouldBe encoder
      parameter.value(0) shouldBe true
      parameter.value(1) shouldBe false
      parameter.units shouldBe UnitsOfMeasure.NoUnits
    }

    List(p3, p5) foreach { parameter ⇒
      parameter.keyName shouldBe encoder
      parameter.value(0) shouldBe true
      parameter.units shouldBe UnitsOfMeasure.NoUnits
    }

    p1 shouldEqual p2
    p2 shouldEqual p4
    p3 shouldEqual p5

  }
}
