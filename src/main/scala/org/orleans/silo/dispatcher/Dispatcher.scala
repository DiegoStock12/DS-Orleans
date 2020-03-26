package org.orleans.silo.dispatcher

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.{ServerSocket, Socket, SocketException}
import java.util.UUID
import java.util.concurrent.{
  ConcurrentHashMap,
  ConcurrentMap,
  Executors,
  LinkedBlockingQueue,
  ThreadPoolExecutor,
  TimeUnit
}

import scala.reflect._
import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.{Master, Slave}
import org.orleans.silo.metrics.{Registry, RegistryFactory}

// TODO how to deal with replicated grains that could have the same ID?
// TODO maybe different mailboxes or a threadpool that distributes the mailbox between the two grains??
/**
  * This thread just takes the messages and puts them in the appropriate mailbox.
  * It gets a message and associates it with the mailbox of that grain
  */
private class MessageReceiver(
    val mailboxIndex: ConcurrentHashMap[String, Mailbox],
    port: Int)
    extends Runnable
    with LazyLogging {

  // Create the socket
  val requestSocket: ServerSocket = new ServerSocket(port)

  //  private val sockets = List[Socket]

  val SLEEP_TIME: Int = 5
  var running: Boolean = true

  // TODO this could be multithreaded but might be too much overload
  /**
    * While true receive messages and put them in the appropriate mailbox
    */
  override def run(): Unit = {
    logger.info(s"Message receiver started in port $port")
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

//      logger.info(
//        s"Accepted new client! ${clientSocket.getInetAddress}: ${clientSocket.getPort}")
      // Important to create the oos if not the ois on the other side of the connection blocks until it is
      val oos: ObjectOutputStream = new ObjectOutputStream(
        clientSocket.getOutputStream)
      val ois: ObjectInputStream = new ObjectInputStream(
        clientSocket.getInputStream)
      val request: Any = ois.readObject()
      // Match the request we just received
      logger.info("received a new request!!!")
      request match {
        // We'll be expecting something like this
        case ((id: String, msg: Any)) =>
          logger.info(
            s"Received message from ${clientSocket.getInetAddress}: ${clientSocket.getPort}")
          logger.info(s"Message = ($id,$msg)")
          if (this.mailboxIndex.containsKey(id)) {
            // Add a message to the queue
            this.mailboxIndex.get(id).addMessage(Message(id, msg, Sender(oos)))
            logger.info(
              s"Increasing the counter for messages received for grain: ${id}")
            val registry: Registry = RegistryFactory.getOrCreateRegistry(id)
            registry.addRequestReceived()
          } else {
            logger.error(s"Not existing mailbox for ID $id")
            this.mailboxIndex.forEach((k, v) => logger.info(s"$k --> $v"))

          }
        case _ =>
          logger.error(s"Received invalid message $request")
      }

      Thread.sleep(SLEEP_TIME)
    }
  }

  def stop(): Unit = {
    logger.debug("Stopping message-receiver.")
    requestSocket.close()
    this.running = false
  }
}

/**
  * Dispatcher that will hold the messages for a certain type of grain
  *
  * @param port port in which the dispatcher will be waiting for requests
  * @tparam T type of the grain that the dispatcher will serve
  */
class Dispatcher[T <: Grain: ClassTag](val port: Int)
    extends Runnable
    with LazyLogging {

  val SLEEP_TIME: Int = 50
  private val THREAD_POOL_DEFAULT_SIZE: Int = 8

  var running: Boolean = true

  // Thread pool to execute new request
  // The thread pool has a variable size that will scale with the number of requests to
  // make it perform better under load while also being efficient in lack of load
  private val pool: ThreadPoolExecutor =
    Executors
      .newFixedThreadPool(THREAD_POOL_DEFAULT_SIZE)
      .asInstanceOf[ThreadPoolExecutor]

  // Maps of Mailbox and grains linking them to an ID
  private[dispatcher] val mailboxIndex: ConcurrentHashMap[String, Mailbox] =
    new ConcurrentHashMap[String, Mailbox]()
  private[dispatcher] val grainMap: ConcurrentMap[Mailbox, Grain] =
    new ConcurrentHashMap[Mailbox, Grain]()

  // Create the message receiver and start it
  private val messageReceiver: MessageReceiver =
    new MessageReceiver(mailboxIndex, port)
  val mRecvThread: Thread = new Thread(messageReceiver)
  mRecvThread.setName(s"MessageReceiver-$port")
  mRecvThread.start()

  logger.info(
    s"Dispatcher for ${classTag[T].runtimeClass.getName} started in port $port")

  /**
    * Creates a new grain and returns its id so it can be referenced
    * by the user and indexed by the master
    */
  def addGrain(): String = {
    // Create the id for the new grain
    val id: String = UUID.randomUUID().toString
    // Create a new grain of that type with the new ID
    val grain: T = classTag[T].runtimeClass
      .getConstructor(classOf[String])
      .newInstance(id)
      .asInstanceOf[T]
    logger.info(s"New grain id $id")
    // Create a mailbox
    val mbox: Mailbox = new Mailbox(grain)
    logger.info(s"New mailbox id $id")

    // Put the new grain and mailbox in the indexes so it can be found
    this.grainMap.put(mbox, grain)
    this.messageReceiver.mailboxIndex.put(id, mbox)

    // Return the id of the grain
    id
  }

  /**
    * Adds a master grain implementation, that will manage the requests for
    * create, delete or search for grains
    * @return
    */
  def addMasterGrain(master: Master): String = {
    // Create the id for the new grain
    val id: String = "master"
    // Create a new grain of that type with the new ID
    val grain: T = classTag[T].runtimeClass
      .getConstructor(classOf[String], classOf[Master])
      .newInstance(id, master)
      .asInstanceOf[T]
    // Create a mailbox
    val mbox: Mailbox = new Mailbox(grain)

    // Put the new grain and mailbox in the indexes so it can be found
    this.grainMap.put(mbox, grain)
    this.messageReceiver.mailboxIndex.put(id, mbox)

    logger.info(s"Created master grain with id $id")
    // Return the id of the grain
    id
  }

  /**
    * Adds the slave grain
    */
  def addSlaveGrain(slave: Slave): String = {
    // Use the same id as the slave
    val id = slave.uuid
    // Create the grain that will manage the slave of that type with the new ID
    val grain: T = classTag[T].runtimeClass
      .getConstructor(classOf[String], classOf[Slave])
      .newInstance(id, slave)
      .asInstanceOf[T]
    // Create a mailbox
    val mbox: Mailbox = new Mailbox(grain)

    // Put the new grain and mailbox in the indexes so it can be found
    this.grainMap.put(mbox, grain)
    this.messageReceiver.mailboxIndex.put(id, mbox)

    // Return the id of the grain
    logger.info(s"Created slave grain with id $id")
    id
  }

  /**
    * Delete a grain and its mailbox
    *
    * @param id
    */
  // TODO we should be careful cause maybe the mailbox is running
  def deleteGrain(id: String) = {
    // delete from index and delete mailbox
    logger.info(s"Deleting information for grain $id")
    this.grainMap.remove(this.messageReceiver.mailboxIndex.get(id))
    this.messageReceiver.mailboxIndex.remove(id)
  }

  override def run(): Unit = {
    while (running) {
      // Iterate through the mailboxes and if one is not empty schedule it
      if (this.grainMap.isEmpty)
        Thread.sleep(SLEEP_TIME)
      this.grainMap.forEach((mbox, _) => {
        if (!mbox.isEmpty && !mbox.isRunning) {
          // if the mailbox is not empty schedule the mailbox
          // Executing the mailbox basically delivers all the messages
          //logger.info(s"Running mailbox ${mbox.id}")
          pool.execute(mbox)
        }
      })
      Thread.sleep(SLEEP_TIME)

    }
  }

  def stop() = {
    logger.debug(
      s"Stopping dispatcher for ${classTag[T].runtimeClass.getClass}.")
    messageReceiver.stop()
    this.running = false
  }

  def getType(): ClassTag[T] = classTag[T]
}
