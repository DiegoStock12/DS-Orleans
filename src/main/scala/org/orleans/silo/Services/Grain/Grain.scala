package org.orleans.silo.Services.Grain

object Grain{
  type Receive = PartialFunction[Any, Unit]
}

abstract class Grain(val _id: String) extends Serializable  {
  def receive : Grain.Receive
  def store() = {
    println(s"Executing store function in grain with id ${_id}")
  }
}
