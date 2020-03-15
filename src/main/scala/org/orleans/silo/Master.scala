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

/**
  * Master silo. Keeps track of all slaves and is the main entry point of the runtime.
  * @param host the host of this server.
  * @param udpPort the UDP port for low-level communication.
  */
class Master(host: String, udpPort: Int = 161)
    extends LazyLogging
    with Runnable
    with PacketListener {

  // Metadata for the master.
  val uuid: String = UUID.randomUUID().toString
  val shortId: String = protocol.shortUUID(uuid)

  @volatile
  var running: Boolean = false
  val SLEEP_TIME: Int = 100

  // Hash table of other slaves. This is threadsafe.
  val slaves = scala.collection.mutable.HashMap[String, SlaveInfo]()

  // Packetmanager which send packets and receives packets (event-driven).
  val packetManager: PacketManager = new PacketManager(this, udpPort)

  /**
    * Starts the master.
    * - Creates a main control loop to keep track of slaves and send heartbeats.
    * - Creates a packet-manager which handles incoming and outgoing packets.
    */
  def start() = {
    logger.info(f"Now starting master with id: ${protocol.shortUUID(uuid)}.")
    this.running = true

    // Starting a packet manager which listens for incoming packets.
    packetManager.init(shortId)

    // Creating master thread and starting it.
    val masterThread = new Thread(this)
    masterThread.setName(f"master-$shortId")
    masterThread.start()
  }

  /** Control loop. */
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
        notifyAllSlaves(heartbeat)

        // Update time
        oldTime = newTime
      }

      // Verify if the slaves are still alive.
      verifySlavesAlive()

      // Now time to sleep :)
      Thread.sleep(SLEEP_TIME)
    }
  }

  /**
    * Send a packet to all slaves.
    * @param packet the packet to send.
    */
  def notifyAllSlaves(packet: Packet): Unit = {
    for ((_, slaveInfo) <- slaves) {
      packetManager.send(packet, slaveInfo.host, slaveInfo.port)
    }
  }

  /**
    * Verifies if all slaves are still alive, otherwise they get removed from the cluster.
    */
  def verifySlavesAlive(): Unit = {
    for ((slaveUUID, slaveInfo) <- slaves) {
      val diffTime = System.currentTimeMillis() - slaveInfo.lastHeartbeat
      if (diffTime >= protocol.deathTime) {
        logger.warn(
          s"Connection to slave ${protocol.shortUUID(slaveUUID)} timed out.")
        removeSlave(slaveUUID)
      }
    }
  }

  /**
    * Remove slave from cluster.
    * @param slaveUUID the uuid to remove.
    */
  def removeSlave(slaveUUID: String): Unit = {
    logger.debug(s"Remove slave ${protocol.shortUUID(slaveUUID)} from cluster.")
    slaves.remove(slaveUUID) // We remove it from the cluster.
  }

  /**
    * Event-driven method which is triggered when a packet is received.
    * Forwards the packet to the correct handler.
    * @param packet the received packet.
    * @param host the host receiving from.
    * @param port the port receiving from.
    */
  override def onReceive(
      packet: Packet,
      host: String,
      port: Int
  ): Unit = packet.packetType match {
    case PacketType.HANDSHAKE => processHandshake(packet, host, port)
    case PacketType.HEARTBEAT => processHeartbeat(packet, host, port)
    case PacketType.SHUTDOWN  => removeSlave(packet.uuid)
    case _                    => logger.warn(s"Did not expect this packet: $packet.")
  }

  /**
    * Processes a handshake.
    * 1) If the slave is already in the cluster, we ignore this packet.
    * 2) Otherwise, add slave to the slaveTable so that it receives heartbeats from the master.
    * 3) Send the slave a 'welcome' packet so that it acknowledges the master.
    *
    * @param packet The handshake packet.
    * @param host The host receiving from.
    * @param port The port receiving from.
    */
  def processHandshake(packet: Packet, host: String, port: Int): Unit = {
    // If slave is already in the cluster, we will not send another welcome packet. Its probably already received.
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

  /**
    * Processes a heartbeat.
    * 1). If the slave is unknown, we ignore this packet.
    *   - It might be that it got rid of this slave because it thought the slave was dead.
    *     After some time, the slave will also consider the master dead and tries to reconnect.
    * 2) Slave information gets updated with the latest heartbeat, so that we know its alive.
    *
    * @param packet The heartbeat packet.
    * @param host The host receiving from.
    * @param port The port receiving from.
    */
  def processHeartbeat(packet: Packet, host: String, port: Int): Unit = {
    if (!slaves.contains(packet.uuid)) {
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

  /**
    * Stopping the master.
    * Returns if it isn't running.
    */
  def stop(): Unit = {
    if (!running) return
    logger.info(f"Now stopping master with id: ${protocol.shortUUID(uuid)}.")

    // Shutdown slaves here.
    logger.debug("Trying to shutdown the slaves.")
    val shutdown =
      Packet(PacketType.SHUTDOWN, this.uuid, System.currentTimeMillis())
    notifyAllSlaves(shutdown)

    // Wait a bit until all slaves are removed.
    Thread.sleep(SLEEP_TIME * 5)

    // Cancel packet manager and control thread.
    this.packetManager.cancel()
    this.running = false
    logger.info("Master exited.")
  }

}
