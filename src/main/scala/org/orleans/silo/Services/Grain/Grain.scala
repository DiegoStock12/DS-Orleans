package org.orleans.silo.Services.Grain
import scala.reflect.ClassTag

object Grain {
  type Receive = PartialFunction[Any, Unit]
}

abstract class Grain(val _id: String) extends Serializable {
  def receive: Grain.Receive
  def store() = {
    println(s"Executing store function in grain with id ${_id}")
  }
}
