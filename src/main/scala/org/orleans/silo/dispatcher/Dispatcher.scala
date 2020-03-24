package org.orleans.silo.dispatcher

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.{ServerSocket, Socket}
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap, Executors, ThreadPoolExecutor}

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.metrics.{Registry, RegistryFactory}


// TODO how to deal with replicated grains that could have the same ID?
// TODO maybe different mailboxes or a threadpool that distributes the mailbox between the two grains??
/**
 * This thread just takes the messages and puts them in the appropriate mailbox.
 * It gets a message and associates it with the mailbox of that grain
 */
private class MessageReceiver(val mailboxIndex: ConcurrentHashMap[String, Mailbox],
                              port: Int)
    extends Runnable
    with LazyLogging {

  // Create the socket
  val requestSocket: ServerSocket = new ServerSocket(port)
  logger.info(s"Message receiver started in port $port")
//  private val sockets = List[Socket]

  // TODO this could be multithreaded but might be too much overload
  /**
   * While true receive messages and put them in the appropriate mailbox
   */
  override def run(): Unit = {
    while (true) {
      // Wait for request
      val clientSocket: Socket = requestSocket.accept
      logger.info(s"Accepted new client! ${clientSocket.getInetAddress}: ${clientSocket.getPort}")
      // Important to create the oos if not the ois on the other side of the connection blocks until it is
      val oos : ObjectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream)
      val ois: ObjectInputStream = new ObjectInputStream(clientSocket.getInputStream)
      val request: Any = ois.readObject()
      // Match the request we just received
      request match {
        // We'll be expecting something like this
        case ((id: String, msg: Any)) =>
          logger.info(s"Received message from ${clientSocket.getInetAddress}:${clientSocket.getPort}")
          logger.info(s"Message = ($id,$msg)")
          if (this.mailboxIndex.containsKey(id)) {
            // Add a message to the queue
            logger.info(s"Adding to queue $id message $msg")
            this.mailboxIndex.get(id).addMessage(Message(id, msg, Sender(oos)))
            logger.info(s"Increasing the counter for messages received for grain: ${id}")
            val registry: Registry = RegistryFactory.getOrCreateRegistry(id)
            registry.addRequestReceived()
            logger.info(s"New size of the queue: ${this.mailboxIndex.get(id).inbox.size()}")
          }
          else{
            logger.error(s"Not existing mailbox for ID $id")
          }
        case _ =>
          logger.error(s"Received invalid message $request")
      }
    }
  }
}

/**
 * Dispatcher that will hold the messages for a certain type of grain
 *
 * @param grain grain to start the dispatcher with
 * @tparam T type of the grain that the dispatcher will serve
 */
class Dispatcher[T <: Grain](grain: T, private val port: Int)
  extends Runnable
    with LazyLogging {
  type GrainType = T

  // Thread pool to execute new request
  // TODO this pool could vary in size so it scales better
  private val pool: ThreadPoolExecutor = Executors.newFixedThreadPool(10).asInstanceOf[ThreadPoolExecutor]

  // Maps of Mailbox and grains linking them to an ID
  private[dispatcher] val mailboxIndex : ConcurrentHashMap[String, Mailbox] = new ConcurrentHashMap[String, Mailbox]()
  private[dispatcher] val grainMap: ConcurrentMap[Mailbox, Grain] = new ConcurrentHashMap[Mailbox, Grain]()

  // Create test mailbox and grains (the grain will have id 1234 passed from the slave)
  val testMbox : Mailbox = new Mailbox(grain)
  println(testMbox)
  // Put the test mailbox in the index and so on
  mailboxIndex.put("1234", testMbox)
  grainMap.put(testMbox, grain)
  println(grain)

  // Create the message receiver and start it
  private val  messageReceiver : MessageReceiver = new MessageReceiver(mailboxIndex, port)
  val mRecvThread : Thread = new Thread(messageReceiver)
  mRecvThread.setName(s"MessageReceiver-$port")
  mRecvThread.start()


  logger.info(s"Dispatcher for ${grain.getClass} started in port $port")


  override def run(): Unit = {

    while (true) {
      // Iterate through the mailboxes and if one is not empty schedule it
      if (this.grainMap.isEmpty)
        Thread.sleep(1000)
      this.grainMap.forEach((mbox, _) => {
        if (!mbox.isEmpty && !mbox.isRunning){
          // if the mailbox is not empty schedule the mailbox
          // Executing the mailbox basically delivers all the messages
          logger.info(s"Running mailbox ${mbox.id}")
          pool.execute(mbox)
        }
      })
      Thread.sleep(50)


    }
  }
}
