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

class Slave(host: String, udpPort: Int = 161, masterConfig: MasterConfig)
    extends LazyLogging
    with Runnable
    with PacketListener {

  // Metadata from the slave
  val uuid: String = UUID.randomUUID().toString
  val shortId: String = uuid.split('-')(0)

  @volatile
  var running: Boolean = false

  @volatile
  var connectedToMaster: Boolean = false

  @volatile
  var masterInfo: MasterInfo = MasterInfo("", 0, masterConfig)

  val packetManager: PacketManager = new PacketManager(this, udpPort)

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

  def stop(): Unit = {
    if (!running) return
    logger.info(f"Now stopping slave with id: $uuid.")

    // clean-up here

    // cancel itself
    this.packetManager.cancel()
    this.running = false
    logger.info("Slave exited.")
  }

  def run(): Unit = {
    var oldTime: Long = System.currentTimeMillis()

    while (running) {
      if (!connectedToMaster) {
        val connected = connectToMaster()
        if (!connected) {
          logger.error("Couldn't connect to master. Now shutting down.")
          this.stop()
          return
        }
      }

      // Keep track of local time, to ensure sending heartbeats on time.
      val newTime: Long = System.currentTimeMillis()
      val timeDiff = newTime - oldTime

      // Check if it is time to send heartbeats again.
      if (timeDiff >= ConnectionProtocol.heartbeatInterval) {
        logger.debug("Sending heartbeats to master.")
        val handshake =
          Packet(PacketType.HEARTBEAT, this.uuid, System.currentTimeMillis())
        packetManager.send(handshake,
                           masterInfo.config.host,
                           masterInfo.config.udpPort)

        // Update time
        oldTime = newTime
      }

      // TODO: HERE SOME LOGIC TO CONFIRM MASTER IS STILL ALIVE

      // Now we sleep again for 100ms.
      Thread.sleep(100)
    }
  }

  def connectToMaster(): Boolean = {
    logger.info("Connecting to master.")
    for (i <- 1 to ConnectionProtocol.connectionAttempts) {
      val handshake =
        new Packet(PacketType.HANDSHAKE, this.uuid, System.currentTimeMillis())
      packetManager.send(handshake, masterConfig.host, masterConfig.udpPort)
      Thread.sleep(ConnectionProtocol.connectionDelay)

      if (connectedToMaster) {
        return true
      }

      logger.info(
        s"Couldn't connect to master. Attempt $i/${ConnectionProtocol.connectionAttempts}.")
    }

    return false
  }

  def processHeartbeat(packet: Packet, host: String, port: Int): Unit = {
    if (packet.uuid != masterInfo.uuid) {
      logger.debug("Received heartbeat packet from an unknown source.")
      return
    }

    // Update latest info on master.
    this.masterInfo =
      masterInfo.copy(lastHeartbeat = System.currentTimeMillis())
  }

  def processWelcome(packet: Packet, host: String, port: Int): Unit = {
    masterInfo =
      MasterInfo(packet.uuid, System.currentTimeMillis(), masterConfig)
    connectedToMaster = true
    logger.info(
      s"Successfully connected to the master with uuid: ${masterInfo.uuid}.")
  }

  override def onReceive(
      packet: Packet,
      host: String,
      port: Int
  ): Unit = packet.packetType match {
    case PacketType.WELCOME   => processWelcome(packet, host, port)
    case PacketType.HEARTBEAT => processHeartbeat(packet, host, port)
    case PacketType.SHUTDOWN  => // Shutdown here
    case _                    => logger.error(s"Did not expect this packet: $packet.")

  }
}
