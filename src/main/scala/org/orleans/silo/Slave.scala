package main.scala.org.orleans.silo
import java.net.{DatagramPacket, DatagramSocket, InetSocketAddress}
import java.nio.charset.StandardCharsets
import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.communication.{
  ConnectionProtocol,
  PacketListener,
  PacketManager
}
import org.orleans.silo.communication.ConnectionProtocol.{
  MasterConfig,
  MasterInfo,
  Packet,
  PacketType
}

/**
  * Slave silo. Handles request from the master.
  * @param host the host of this server.
  * @param udpPort the UDP port for low-level communication.
  * @param masterConfig configuration to connect to the master.
  */
class Slave(host: String, udpPort: Int = 161, masterConfig: MasterConfig)
    extends LazyLogging
    with Runnable
    with PacketListener {

  // Metadata for the slave.
  val uuid: String = UUID.randomUUID().toString
  val shortId: String = uuid.split('-')(0)

  @volatile
  var running: Boolean = false
  val SLEEP_TIME: Int = 100

  // Master information.
  @volatile
  var connectedToMaster: Boolean = false
  @volatile
  var masterInfo: MasterInfo = MasterInfo("", 0)

  // Packetmanager which send packets and receives packets (event-driven).
  val packetManager: PacketManager = new PacketManager(this, udpPort)

  /**
    * Starts the slave.
    * - Creates a main control loop to send information to the master.
    * - Creates a packet-manager which handles incoming and outgoing packets.
    */
  def start() = {
    logger.info(f"Now starting slave with id: $uuid.")
    this.running = true

    // Starting a packet manager which listens for incoming packets.
    packetManager.init(shortId)

    // Creating slave thread and starting it.
    val slaveThread = new Thread(this)
    slaveThread.setName(f"slave-$shortId")
    slaveThread.start()
  }

  /** Control loop. */
  def run(): Unit = {
    var oldTime: Long = System.currentTimeMillis()

    while (running) {
      // If not connected to the master, lets do a handshake.
      if (!connectedToMaster) {
        connectToMaster()
      }

      // Keep track of local time, to ensure sending heartbeats on time.
      val newTime: Long = System.currentTimeMillis()
      val timeDiff = newTime - oldTime

      // Check if it is time to send heartbeats again.
      if (timeDiff >= ConnectionProtocol.heartbeatInterval) {
        logger.debug("Sending heartbeats to master.")

        // Send heartbeat to the master.
        val heartbeat = Packet(PacketType.HEARTBEAT, this.uuid, newTime)
        packetManager.send(heartbeat, masterConfig.host, masterConfig.udpPort)

        // Update time
        oldTime = newTime
      }

      // TODO: HERE SOME LOGIC TO CONFIRM MASTER IS STILL ALIVE

      // Now time to sleep :)
      Thread.sleep(100)
    }
  }

  /**
    * Trying to connect to master.
    * 1) Sends handshake packet.
    * 2) Expects welcome packet which toggles `connectedToMaster` flag.
    *   - If not received within `connectionDelay`ms, then retry `conectionAttempts` times.
    *   - If no welcome packet is received at all. Slave silo is shut down.
    */
  def connectToMaster(): Unit = {
    logger.info("Connecting to master.")
    for (i <- 1 to ConnectionProtocol.connectionAttempts) {
      // Send a handshake and wait for a bit.
      val handshake =
        new Packet(PacketType.HANDSHAKE, this.uuid, System.currentTimeMillis())
      packetManager.send(handshake, masterConfig.host, masterConfig.udpPort)
      Thread.sleep(ConnectionProtocol.connectionDelay)

      // If connected, the slave can start its life.
      if (connectedToMaster) {
        return
      }

      logger.info(
        s"Couldn't connect to master. Attempt $i/${ConnectionProtocol.connectionAttempts}.")
    }

    // If couldn't connect to the master after x attempts, shutdown server.
    logger.error("Couldn't connect to master. Now shutting down.")
    this.stop()
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
    case PacketType.WELCOME   => processWelcome(packet, host, port)
    case PacketType.HEARTBEAT => processHeartbeat(packet, host, port)
    case PacketType.SHUTDOWN  => // TODO: SHUTDOWN HERE
    case _                    => logger.error(s"Did not expect this packet: $packet.")

  }

  /**
    * Processes a welcome.
    * 1). If a welcome packet is received, the `masterInfo` is updated with the correct UUID.
    *
    * @param packet The welcome packet.
    * @param host The host receiving from.
    * @param port The port receiving from.
    */
  def processWelcome(packet: Packet, host: String, port: Int): Unit = {
    masterInfo = MasterInfo(packet.uuid, System.currentTimeMillis())
    connectedToMaster = true
    logger.info(
      s"Successfully connected to the master with uuid: ${masterInfo.uuid}.")
  }

  /**
    * Processes a heartbeat.
    * 1). If the packet is from an unknown UUID, we ignore this packet.
    *   - TODO I'm not sure if this is already a good thing, what if the master crashed and got a new UUID?
    * 2) Master information gets updated with the latest heartbeat, so that we know its alive.
    *
    * @param packet The heartbeat packet.
    * @param host The host receiving from.
    * @param port The port receiving from.
    */
  def processHeartbeat(packet: Packet, host: String, port: Int): Unit = {
    if (packet.uuid != masterInfo.uuid) {
      logger.debug("Received heartbeat packet from an unknown source.")
      return
    }

    // Update latest info on master.
    this.masterInfo =
      masterInfo.copy(lastHeartbeat = System.currentTimeMillis())
  }

  /**
    * Stopping the slave.
    * Returns if it isn't running.
    */
  def stop(): Unit = {
    if (!running) return
    logger.info(f"Now stopping slave with id: $uuid.")

    // TODO Clean-up here.

    // cancel itself
    this.packetManager.cancel()
    this.running = false
    logger.info("Slave exited.")
  }
}
