package main.scala.org.orleans.silo
import java.net.{DatagramPacket, DatagramSocket, InetAddress, InetSocketAddress}
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.{Date, UUID}

import org.orleans.silo.communication.{
  PacketListener,
  PacketManager,
  ConnectionProtocol => protocol
}
import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.communication.ConnectionProtocol.{
  Packet,
  PacketType,
  SlaveInfo
}

class Master(host: String, udpPort: Int = 161)
    extends LazyLogging
    with Runnable
    with PacketListener {

  // Metadata from the master
  val uuid: String = UUID.randomUUID().toString
  val shortId: String = uuid.split('-')(0)

  @volatile
  var running: Boolean = false

  // Hash table of other slaves
  val slaves = scala.collection.mutable.HashMap[String, SlaveInfo]()

  val packetManager: PacketManager = new PacketManager(this, udpPort)

  /**
    * Gets invoked when a master is started.
    */
  def start() = {
    logger.info(f"Now starting master with id: $uuid.")
    this.running = true

    // Starting a packet manager which listens for incoming packets.
    packetManager.init(shortId)

    // Creating master thread and starting it.
    val masterThread = new Thread(this)
    masterThread.setName(f"master-$shortId")
    masterThread.start()
  }

  def run(): Unit = {
    var oldTime: Long = System.currentTimeMillis()

    while (this.running) {
      // Keep track of local time, to ensure sending heartbeats on time.
      val newTime: Long = System.currentTimeMillis()
      val timeDiff = newTime - oldTime

      // Check if it is time to send heartbeats again.
      if (timeDiff >= protocol.heartbeatInterval) {
        logger.debug("Sending heartbeats to slaves.")

        // Send its heartbeat to all slaves.
        val heartbeat = Packet(PacketType.HEARTBEAT, this.uuid, newTime)
        slaves.foreach(slave =>
          packetManager.send(heartbeat, slave._2.host, slave._2.port))

        // Update time
        oldTime = newTime
      }

      // TODO: HERE SOME LOGIC TO CHECK IF SLAVES ARE STILL ALIVE

      // Now we sleep again for 100ms.
      Thread.sleep(100)
    }
  }

  def processHandshake(packet: Packet, host: String, port: Int): Unit = {
    // If slave is already in the quorum, we will not send another welcome packet. Its probably already received.
    if (slaves.contains(packet.uuid)) return
    logger.debug(s"Adding new slave to the cluster.")

    // First we add it to the slaves table.
    val slaveInfo = SlaveInfo(packet.uuid, host, port, packet.timestamp)
    slaves.put(slaveInfo.uuid, slaveInfo)

    // Then we send the slave its welcome packet :)
    val welcome =
      Packet(PacketType.WELCOME, this.uuid, System.currentTimeMillis())
    packetManager.send(welcome, host, port)

    logger.debug(s"Slave with ${slaveInfo.uuid} is added to the cluster.")
  }

  def processHeartbeat(packet: Packet, host: String, port: Int): Unit = {
    if (slaves.contains(packet.uuid)) {
      logger.debug(
        "Got a heartbeat from an unknown slave. Probably it has been disconnected in the past.")
      return
    }

    // Update the slaveInfo with the current time.
    val slaveInfo = slaves
      .get(packet.uuid)
      .get
      .copy(lastHeartbeat = System.currentTimeMillis())
    slaves.put(packet.uuid, slaveInfo)
  }

  override def onReceive(
      packet: Packet,
      host: String,
      port: Int
  ): Unit = packet.packetType match {
    case PacketType.HANDSHAKE => processHandshake(packet, host, port)
    case PacketType.HEARTBEAT => processHeartbeat(packet, host, port)
    case _                    => logger.error(s"Did not expect this packet: $packet.")
  }

  def stop(): Unit = {
    if (!running) return
    logger.info(f"Now stopping master with id: $uuid.")

    // shutdown slaves here
    logger.info("Trying to shutdown the slaves.")

    // clean-up here

    // cancel itself
    this.packetManager.cancel()
    this.running = false
    logger.info("Master exited.")
  }

}
