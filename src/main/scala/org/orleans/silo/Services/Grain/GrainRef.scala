package org.orleans.silo.Services.Grain

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.Socket

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.GrainRef.SenderInfo

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

object GrainRef extends LazyLogging{
  def apply(id: String, address: String, port: Int): GrainRef = {
    // Create a socket
    s = new Socket(address, port)
    // Important to create the output stream before the input stream!
    val oos: ObjectOutputStream = new ObjectOutputStream(s.getOutputStream)
    val ois: ObjectInputStream = new ObjectInputStream(s.getInputStream)
    new GrainRef(oos, ois, id)
  }

  case class SenderInfo(address: String, port: Int)

  var s: Socket = _
}

// TODO maybe for fire and forget we could use DatagramSocket, but then
// we could not be sure that it has been received
class GrainRef private(val outStream: ObjectOutputStream, val inStream: ObjectInputStream, val id: String) {

  import GrainRef._

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
    outStream.writeObject((id, msg))
  }

  /**
   * Method to wait for the response from the server
   *
   * @param msg message to send to the server
   * @param id  id of the grain to send to
   * @return
   */
  def ?(msg: Any)(implicit id: String = id): Future[Any] = sendWithResponse(msg, id)

  /**
   * Returns a Future with the response from the server
   *
   * @param msg
   * @param id
   * @return
   */
  // TODO still not able to set a way so the other grain responds
  private[this] def sendWithResponse(msg: Any, id: String): Future[Any] = {
    outStream.writeObject((id, msg))
    Future {
      val resp: Any = inStream.readObject()
      resp
    }
  }

}
