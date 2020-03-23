package org.orleans.silo.Services.Grain

import com.google.protobuf.GeneratedMessage
import scalapb.GeneratedMessage

abstract class Grain(val _id: String) extends Serializable  {
  type Reply
  type Request
  def receive(request: Request) : Reply
  def store()
}
