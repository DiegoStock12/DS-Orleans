package org.orleans.silo.dispatcher
import java.io.{
  EOFException,
  IOException,
  ObjectInputStream,
  ObjectOutputStream
}
import java.net.{ServerSocket, Socket, SocketException}
import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.metrics.{Registry, RegistryFactory}

// TODO how to deal with replicated grains that could have the same ID?
// TODO maybe different mailboxes or a threadpool that distributes the mailbox between the two grains??
/**
  * This thread just takes the messages and puts them in the appropriate mailbox.
  * It gets a message and associates it with the mailbox of that grain
  */
class MessageReceiver(val mailboxIndex: ConcurrentHashMap[String, Mailbox],
                      client: Socket)
    extends Runnable
    with LazyLogging {
  val SLEEP_TIME: Int = 5
  var running: Boolean = true
  var lastReceivedMessage: Long = System.currentTimeMillis()

  /**
    * While true receive messages and put them in the appropriate mailbox.
    */
  override def run(): Unit = {
    logger.info(
      s"Starting receiving messages for ${client.getInetAddress}:${client.getPort}.")
    val oos: ObjectOutputStream = new ObjectOutputStream(client.getOutputStream)
    val ois: ObjectInputStream = new ObjectInputStream(client.getInputStream)
    lastReceivedMessage = System.currentTimeMillis()
    while (running) {
      var request: Any = null
      try {
        request = ois.readObject()
        lastReceivedMessage = System.currentTimeMillis()
      } catch {
        case exception: IOException => {
          running = false
          return
        }
      }
      // Match the request we just received
      request match {
        // We'll be expecting something like this
        case ((requestId: String, grainId: String, msg: Any)) =>
          if (this.mailboxIndex.containsKey(grainId)) {
            // Add a message to the queue
            this.mailboxIndex
              .get(grainId)
              .addMessage(Message(grainId, msg, Sender(oos, requestId)))
            val registry: Registry =
              RegistryFactory.getOrCreateRegistry(grainId)
            registry.addRequestReceived()
          } else {
            logger.error(s"Not existing mailbox for ID $grainId")
            this.mailboxIndex.forEach((k, v) => logger.error(s"$k --> $v"))

          }
        case _ =>
          logger.error(s"Received invalid message $request")
      }

      logger.debug("Processed a request. Now start waiting again.")
    }
  }

  def isRunning() = running

  def stop(): Unit = {
    logger.debug("Stopping message-receiver.")
    if (client.isConnected) {
      client.close()
    }
    this.running = false
  }
}
