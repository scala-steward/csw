package csw.location.api.models.scaladsl

import csw.location.api.models.ComponentType
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class ComponentTypeTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  // DEOPSCSW-14: Codec for data model
  test(
    "ComponentType should be any one of this types : 'Container', 'HCD', 'Assembly', 'Service', 'Machine', 'Sequencer' and  'SequenceComponent' | DEOPSCSW-14"
  ) {
    val expectedComponentTypeValues = Set("Container", "HCD", "Assembly", "Service", "Machine", "Sequencer", "SequenceComponent")
    val actualComponentTypeValues: Set[String] =
      ComponentType.values.map(componentType => componentType.entryName).toSet

    actualComponentTypeValues shouldEqual expectedComponentTypeValues
  }
}
