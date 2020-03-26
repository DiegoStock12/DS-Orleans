package org.orleans.silo.Services.Grain

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.Socket

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object GrainRef extends LazyLogging {
  def apply(id: String, address: String, port: Int): GrainRef =
    new GrainRef(id, address, port)

}

trait GrainReference {
  var grainRef: GrainRef = _
  var masterRef: GrainRef = _
  def setGrainRef(grainRef: GrainRef) = this.grainRef = grainRef
  def setMasterGrain(master: GrainRef) = this.masterRef = master
}

// TODO maybe for fire and forget we could use DatagramSocket, but then
// we could not be sure that it has been received
class GrainRef private (val id: String, val address: String, val port: Int)
    extends LazyLogging {

  private var s: Socket = _
  private var outStream: ObjectOutputStream = _
  private var inStream: ObjectInputStream = _

  /**
    * Send the request to the grain without waiting for a response
    *
    * @param msg message to send
    * @param id  id of the grain that we're making reference to
    */
  def !(msg: Any)(implicit id: String = id) = sendMessage(msg, id)

  /**
    * Sends the message to the specified address and port
    *
    * @param msg
    * @param id
    */
  private[this] def sendMessage(msg: Any, id: String) = {
    s = new Socket(address, port)
    outStream = new ObjectOutputStream(s.getOutputStream)
    inStream = new ObjectInputStream(s.getInputStream)
    logger.info(s"Sending message ${(id, msg)} to $address:$port")
    outStream.writeObject((id, msg))
    println("here now")
  }

  /**
    * Method to wait for the response from the server
    *
    * @param msg message to send to the server
    * @param id  id of the grain to send to
    * @return
    */
  def ?(msg: Any)(implicit id: String = id): Future[Any] =
    sendWithResponse(msg, id)

  /**
    * Returns a Future with the response from the server
    *
    * @param msg
    * @param id
    * @return
    */
  // TODO still not able to set a way so the other grain responds
  private[this] def sendWithResponse(msg: Any, id: String): Future[Any] = {
    s = new Socket(address, port)
    outStream = new ObjectOutputStream(s.getOutputStream)
    inStream = new ObjectInputStream(s.getInputStream)
    outStream.writeObject((id, msg))
    Future {
      val resp: Any = inStream.readObject()
      resp
    }
  }

  override def equals(obj: Any): Boolean = {
    if (!obj.isInstanceOf[GrainRef]) return false
    val ref = obj.asInstanceOf[GrainRef]
    this.id == ref.id && this.address == ref.address && this.port == ref.port
  }

}
