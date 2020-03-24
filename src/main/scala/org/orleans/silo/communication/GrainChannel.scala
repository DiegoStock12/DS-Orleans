package org.orleans.silo.communication
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import org.orleans.silo.Services.Grain.Grain

import scala.concurrent.Future

sealed abstract class Message(id: String)
case class GrainMessage(id: String, grainUUID: String, msg: Any) extends Message(id)
case class InfoMessage(id: String, msg: String) extends Message(id)

class GrainChannel[G <: Grain](host: String, port: Int) {

  val map: ConcurrentHashMap[String, Future] = new ConcurrentHashMap[String, Future]()



  def send[T, G](grainID: String, msg: T): Future[T] = {
    val sendId = UUID.randomUUID().toString

    //send it here
    val messageToSend = GrainMessage(sendId, grainID, msg)


    return null
  }

  def receive(message: Message) = message match {
  }

}
