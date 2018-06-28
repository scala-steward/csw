package csw.messages.commands

import ai.x.play.json.Jsonx
import play.api.libs.json.Format
import upickle.default.{ReadWriter => RW, macroRW}

//sealed trait IntOrTuple
//object IntOrTuple {
//  implicit def rw: Format[IntOrTuple] = Jsonx.formatSealed[IntOrTuple]
//}
//case class IntThing(i: Int) extends IntOrTuple
//object IntThing {
//  implicit def rw: Format[IntThing] = Jsonx.formatCaseClass[IntThing]
//}
//case class TupleThing(name: String, t: (Int, Int)) extends IntOrTuple
//object TupleThing {
//  implicit def rw: Format[TupleThing] = Jsonx.formatCaseClass[TupleThing]
//}

sealed trait IntOrTuple
object IntOrTuple {
  implicit def rw: RW[IntOrTuple] = RW.merge(IntThing.rw, TupleThing.rw)
}
case class IntThing(i: Int) extends IntOrTuple
object IntThing {
  implicit def rw: RW[IntThing] = macroRW
}
case class TupleThing(name: String, t: (Int, Int)) extends IntOrTuple
object TupleThing {
  implicit def rw: RW[TupleThing] = macroRW
}
