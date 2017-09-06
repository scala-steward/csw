package csw.param.generics

import csw.param.commands.{CommandInfo, Setup}
import csw.param.events.{EventInfo, EventTime}
import csw.param.models.ObsId
import org.scalatest.{FunSpec, Matchers}

class ParamsApiHole extends FunSpec with Matchers {

  //make 2 keys
  val encoder: Key[Int] = KeyType.IntKey.make("encoder")
  val filter: Key[Int]  = KeyType.IntKey.make("filter")

  //make 3 parameters with encoder key
  val eP1 = encoder.set(1)
  val eP2 = encoder.set(2)
  val eP3 = encoder.set(3)

  //make 3 parameters with filter key
  val fP1 = filter.set(1)
  val fP2 = filter.set(2)
  val fP3 = filter.set(3)

  val commandInfo = CommandInfo(ObsId("Obs001"))
  val eventInfo   = EventInfo("wfos.blue.filter", EventTime(), None)
  val prefix      = "wfos.blue.filter"

  describe("Test API holes") {
    it("madd/add should not allow parameters with duplicate keys") {

      //parameters with duplicate key via constructor
      val setup = Setup(commandInfo, prefix)
      setup.paramSet.toList.map(_.keyName) should contain theSameElementsAs List()

      //parameters with duplicate key via add + madd. If keyName must be unique then eP2 and fP2 are duplicate
      val changedSetup = setup.add(eP3).madd(eP1, eP2, fP1, fP2)
      //set must hold 2 unique keys
      changedSetup.paramSet.size shouldBe 2
      changedSetup.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoder.keyName, filter.keyName)

      //more duplicate with add
      val finalSetUp = setup.add(eP2).add(fP2).add(eP3).add(fP3)
      //set must hold 2 unique keys
      changedSetup.paramSet.size shouldBe 2
      finalSetUp.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoder.keyName, filter.keyName)
    }

    it("Constructor should not allow parameters with duplicate keys - sc1") {

      //parameters with duplicate key via constructor
      val setup = Setup(commandInfo, prefix, Set(eP1, eP2, fP1, fP2))
      //set must hold 2 unique keys
      setup.paramSet.size shouldBe 2
      setup.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoder.keyName, filter.keyName)
    }

    it("Constructor should not allow parameters with duplicate keys - sc2") {

      //parameters with duplicate key via constructor
      val setup = Setup(commandInfo, prefix, Set(eP1, eP2, fP1, fP2))
      //set must hold unique keys
      setup.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoder.keyName, filter.keyName)
    }

  }
}
