package main.scala.org.orleans.silo
import java.net.DatagramSocket
import java.util.{Date, UUID}

import com.typesafe.scalalogging.LazyLogging

case class SlaveInfo(uuid: String,
                     ip: String,
                     port: String,
                     lastHeartbeat: Long)

class Master(ip: String, port: Int = 161) extends LazyLogging with Runnable {

  // Metadata from the master
  private val uuid: String = UUID.randomUUID().toString

  private val socket: DatagramSocket = new DatagramSocket(port)

  @volatile
  private var running: Boolean = false

  // Sleep time of 'main-master'
  private val SLEEP_TIME: Int = 1000

  // Hash table of other slaves
  val slaves = scala.collection.mutable.HashMap[String, SlaveInfo]()

  /**
    * Gets invoked when a master is started.
    */
  def start() = {
    logger.info(f"Now starting master with id: $uuid.")
    this.running = true

    // Creating master thread and starting it.
    val masterThread = new Thread(this)
    masterThread.setName("master")
    masterThread.start()

  }

  def stop() = {
    logger.info(f"Now stopping master with id: $uuid.")

    // shutdown slaves here
    logger.info("Trying to shutdown the slaves.")

    // cancel itself
    this.running = false
    logger.info("Master exited.")
  }

  def run(): Unit = {
    while (running) {
      logger.debug("I'm alive!")

      Thread.sleep(SLEEP_TIME)
    }
  }

  def parsePacket(packet: Array[Byte]) = {}
}
