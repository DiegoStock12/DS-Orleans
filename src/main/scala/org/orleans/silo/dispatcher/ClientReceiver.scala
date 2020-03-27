package org.orleans.silo.dispatcher
import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.{ServerSocket, Socket, SocketException}
import java.util
import java.util.{Collections, Timer, TimerTask}
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain

import scala.reflect._
import org.orleans.silo.metrics.{Registry, RegistryFactory}

import collection.JavaConverters._

class ClientCleanup(clientSockets: util.List[MessageReceiver])
    extends TimerTask
    with LazyLogging {
  val CLIENT_REMOVE_TIME_SEC: Int = 10
  override def run(): Unit = {
    val toRemove: util.List[MessageReceiver] =
      new util.ArrayList[MessageReceiver]()

    for (client: MessageReceiver <- clientSockets.asScala) {
      if (!client.isRunning()) {
        toRemove.add(client)
      }

      val timeDiff = System.currentTimeMillis() - client.lastReceivedMessage
      if (TimeUnit.MILLISECONDS.toSeconds(timeDiff) >= CLIENT_REMOVE_TIME_SEC) {
        toRemove.add(client)
        client.stop()
      }
    }

    clientSockets.removeAll(toRemove)
    if (toRemove.size() > 0) {
      logger.info(s"Cleaned up ${toRemove
        .size()} message receiver threads.")
    }
  }
}
class ClientReceiver[T <: Grain: ClassTag](
    val mailboxIndex: ConcurrentHashMap[String, Mailbox],
    port: Int)
    extends Runnable
    with LazyLogging {

  // Create the socket
  val requestSocket: ServerSocket = new ServerSocket(port)

  val timer: Timer = new Timer(
    s"ClientCleaner - ${classTag[T].runtimeClass.getSimpleName}")

  //List of
  val clientSockets: util.List[MessageReceiver] =
    Collections.synchronizedList(new util.ArrayList[MessageReceiver]())

  val SLEEP_TIME: Int = 5
  var running: Boolean = true

  // TODO this could be multithreaded but might be too much overload
  /**
    * While true receive messages and put them in the appropriate mailbox
    */
  override def run(): Unit = {
    logger.info(s"Client-Receiver started for ${classTag[T]} on port $port")

    timer.scheduleAtFixedRate(new ClientCleanup(clientSockets), 0, 1000)

    while (running) {
      // Wait for request
      var clientSocket: Socket = null

      try {
        clientSocket = requestSocket.accept
      } catch {
        case socket: SocketException =>
          logger.warn(socket.getMessage)
          return //socket is probably closed, we can exit this method.
      }

      val messageReceiver = new MessageReceiver(mailboxIndex, clientSocket)
      val mRecvThread: Thread = new Thread(messageReceiver)
      logger.info(s"Message-Receiver started on ${clientSocket.getPort}.")
      mRecvThread.setName(
        s"Receiver for ${clientSocket.getInetAddress}:${clientSocket.getPort}.")
      mRecvThread.start()

      clientSockets.add(messageReceiver)
    }
  }

  def stop(): Unit = {
    logger.debug("Stopping client-receiver.")
    for (client: MessageReceiver <- clientSockets.asScala) {
      client.stop()
    }
    timer.cancel()
    requestSocket.close()
    this.running = false
  }
}
