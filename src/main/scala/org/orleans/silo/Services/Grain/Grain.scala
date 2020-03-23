package org.orleans.silo.Services.Grain

import com.google.protobuf.GeneratedMessage
import scalapb.GeneratedMessage

object Grain{
  type Receive = PartialFunction[Any, Unit]
}

abstract class Grain(val _id: String) extends Serializable  {
  type Reply
  type Request
  def receive : Grain.Receive
  def store()
}
