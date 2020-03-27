package org.orleans.silo.Services.Grain

import java.io.{
  EOFException,
  IOException,
  ObjectInputStream,
  ObjectOutputStream
}
import java.net.Socket
import java.util
import java.util.{Collections, UUID}
import java.util.concurrent.{ArrayBlockingQueue, ConcurrentHashMap}

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import collection.JavaConverters._
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
    extends LazyLogging
    with Runnable {

  private var s: Socket = _
  private var outStream: ObjectOutputStream = _
  private var inStream: ObjectInputStream = _
  private var currentListener: Thread = _
  private var connectionOpened = false

  private val expectedMessages: ConcurrentHashMap[String, Promise[Any]] =
    new ConcurrentHashMap[String, Promise[Any]]()

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
    verifyConnection()
    try {
      outStream.writeObject(("", id, msg))
    } catch {
      case exception: Exception => println(exception)

    }
    outStream.flush()
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
    verifyConnection()
    val uuid = UUID.randomUUID().toString
    val promise = Promise[Any]()
    expectedMessages.put(uuid, promise)
    outStream.writeObject((uuid, id, msg))
    outStream.flush()

    promise.future
  }

  def run(): Unit = {
    verifyConnection()

    while (connectionOpened) {
      try {
        var incoming: (String, Any) = null
        incoming = inStream.readObject().asInstanceOf[(String, Any)]

        if (expectedMessages.size() == 0) {
          logger.warn(s"Received an message that wasn't expected: ${incoming}.")
        } else {
          if (expectedMessages.containsKey(incoming._1)) {
            expectedMessages.get(incoming._1).success(incoming._2)
            expectedMessages.remove(incoming._1)
          } else {
            logger.warn(
              s"Received an message that wasn't expected: ${incoming}.")
          }

        }
      } catch {
        case exception: IOException => {
          connectionOpened = false
          return
        }
      }
    }
  }

  def verifyConnection(): Unit = {
    if (!connectionOpened) {
      connectionOpened = true
      s = new Socket(address, port)
      outStream = new ObjectOutputStream(s.getOutputStream)
      inStream = new ObjectInputStream(s.getInputStream)

      currentListener = new Thread(this)
      currentListener.start()
    }
  }

  def closeConnection(): Unit = {
    outStream.flush()
    inStream.close()
    s.close()

    connectionOpened = false
  }

  override def equals(obj: Any): Boolean = {
    if (!obj.isInstanceOf[GrainRef]) return false
    val ref = obj.asInstanceOf[GrainRef]
    this.id == ref.id && this.address == ref.address && this.port == ref.port
  }

}
