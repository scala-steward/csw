package csw.common.params.generics

import com.twitter.chill.KryoInjection

/**
 * Defines methods for serializing parameter sets
 */
private[common] object ParamSetSerializer {
  def read[A](bytes: Array[Byte]): A = KryoInjection.invert(bytes).get.asInstanceOf[A]
  def write[A](in: A): Array[Byte]   = KryoInjection(in)
}
