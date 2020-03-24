package org.orleans.silo
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.LazyLogging
import io.grpc.{Server, ServerBuilder}
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.Services.Impl.{
  CreateGrainImpl,
  GrainSearchImpl,
  UpdateStateServiceImpl
}
import org.orleans.silo.communication.ConnectionProtocol.{
  Packet,
  PacketType,
  SlaveInfo
}
import org.orleans.silo.communication.{
  PacketListener,
  PacketManager,
  ConnectionProtocol => protocol
}
import org.orleans.silo.createGrain.CreateGrainGrpc
import org.orleans.silo.grainSearch.GrainSearchGrpc
import org.orleans.silo.runtime.Runtime
import org.orleans.silo.updateGrainState.UpdateGrainStateServiceGrpc
import org.orleans.silo.utils.{
  GrainDescriptor,
  GrainState,
  ServerConfig,
  SlaveDetails
}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag
import scala.reflect._

object MasterBuilder {
  def apply(): MasterBuilder = new MasterBuilder()
}
class MasterBuilder extends LazyLogging {

  private var serverConfig: ServerConfig = null
  private var executionContext: ExecutionContext = null
  private var grains: mutable.MutableList[ClassTag[_ <: Grain]] =
    mutable.MutableList()

  def setServerConfig(serverConfig: ServerConfig): MasterBuilder = {
    this.serverConfig = serverConfig
    this
  }

  def setExecutionContext(executionContext: ExecutionContext): MasterBuilder = {
    this.executionContext = executionContext
    this
  }

  def registerGrain[T <: Grain: ClassTag] = {
    val tag = classTag[T]

    if (this.grains.contains(tag)) {
      logger.warn(s"${tag.runtimeClass.getName} already registered in master.")
    }

    this.grains += classTag[T]
  }

  def build(): Master = {
    if (serverConfig == null)
      throw new IllegalArgumentException("Master has no serverconfig set.")

    if (executionContext == null) {
      logger.warn(
        "Master has no execution context set. Will use the global one.")
      this.executionContext = ExecutionContext.global
    }

    new Master(serverConfig, executionContext, grains.toList)
  }
}

/**
  * Master silo. Keeps track of all slaves and is the main entry point of the runtime.
  * @param masterConfig Server configuration for the master
  * @param executionContext Execution context for the RPC services
  */
class Master(masterConfig: ServerConfig,
             executionContext: ExecutionContext,
             registeredGrains: List[ClassTag[_ <: Grain]] = List())
    extends LazyLogging
    with Runnable
    with PacketListener {

  // For now just define it as a gRPC endpoint
  self =>
  private[this] var master: Server = null

  // Hashmap to save the grain references
  private val grainMap: ConcurrentHashMap[String, List[GrainDescriptor]] =
    new ConcurrentHashMap[String, List[GrainDescriptor]]()

  // Metadata for the master.
  val uuid: String = UUID.randomUUID().toString
  val shortId: String = protocol.shortUUID(uuid)

  @volatile
  var running: Boolean = false
  val SLEEP_TIME: Int = 100

  // Hash table of other slaves. This is threadsafe.
  val slaves = scala.collection.mutable.HashMap[String, SlaveInfo]()

  // Packetmanager which send packets and receives packets (event-driven).
  val packetManager: PacketManager =
    new PacketManager(this, masterConfig.udpPort)

  // Runtime object that keeps track of grain activity
  val runtime: Runtime = new Runtime(masterConfig, "master", false)

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

    // Start runtime thread
//    val runtimeThread = new Thread(runtime)
//    runtimeThread.setName("runtime")
//    runtimeThread.start()

    startgRPC()
  }

  /**
    * Starts the gRPC server.
    */
  def startgRPC() = {
    grainMap.put("diegoalbo",
                 List(
                   GrainDescriptor(GrainState.Activating,
                                   SlaveDetails("localhost", 50400))))
    master = ServerBuilder
      .forPort(masterConfig.rpcPort)
      .addService(GrainSearchGrpc.bindService(new GrainSearchImpl(grainMap),
                                              executionContext))
      .addService(UpdateGrainStateServiceGrpc
        .bindService(new UpdateStateServiceImpl(grainMap), executionContext))
      .addService(CreateGrainGrpc
        .bindService(new CreateGrainImpl("master", runtime), executionContext))
      .build
      .start

    logger.info(
      "Master server started, listening on port " + masterConfig.udpPort)
    sys.addShutdownHook {
      logger.error("*** shutting down gRPC server since JVM is shutting down")
      this.stop()
    }
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
    * Send a packet to all slaves (exluding the slaves from the except list).
    * @param packet the packet to send.
    * @param except: the slaves not to send to.
    */
  def notifyAllSlaves(packet: Packet, except: List[String] = List()): Unit = {
    for ((_, slaveInfo) <- slaves) {
      if (!except.contains(slaveInfo.uuid)) {
        packetManager.send(packet, slaveInfo.host, slaveInfo.port)
      }
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
    case PacketType.SHUTDOWN  => processShutdown(packet, host, port)
    case _                    => logger.warn(s"Did not expect this packet: $packet.")
  }

  /**
    * Processes a handshake.
    * 1) If the slave is already in the cluster, we ignore this packet.
    * 2) Otherwise, add slave to the slaveTable so that it receives heartbeats from the master.
    * 3) Send the slave a 'welcome' packet so that it acknowledges the master.
    * 4) Send all other slaves there is a new slave in the cluster.
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

    // And send all other slaves in the cluster there is a new slave.
    val new_slave = Packet(PacketType.SLAVE_CONNECT,
                           slaveInfo.uuid,
                           System.currentTimeMillis(),
                           List(slaveInfo.host, slaveInfo.port.toString))
    notifyAllSlaves(new_slave, except = List(slaveInfo.uuid))

    // Finally send this slave awareness of all other slaves.
    for ((slaveUUID, otherSlaveInfo) <- slaves) {
      if (slaveUUID != slaveInfo.uuid) {
        val slavePacket = Packet(
          PacketType.SLAVE_CONNECT,
          otherSlaveInfo.uuid,
          System.currentTimeMillis(),
          List(otherSlaveInfo.host, otherSlaveInfo.port.toString))

        packetManager.send(slavePacket, host, port)
      }
    }

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
    * Processes a shutdown of a slave.
    * 1) Remove the slave from its own table.
    * 2) Make other slaves aware this slave is removed.
    * @param packet The shutdown packet.
    * @param host The host receiving from.
    * @param port The port receiving from.
    */
  def processShutdown(packet: Packet, host: String, port: Int): Unit = {
    // Remove the slave.
    removeSlave(packet.uuid)

    // Notify all others the slave has been removed.
    val disconnect = Packet(PacketType.SLAVE_DISCONNECT,
                            packet.uuid,
                            System.currentTimeMillis())
    notifyAllSlaves(disconnect)
  }

  /** Returns all slaves. **/
  def getSlaves(): List[SlaveInfo] = slaves.toList.map(_._2)

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
    if (master != null) {
      master.shutdown()
    }
    this.packetManager.cancel()
    this.running = false
    this.slaves.clear()
    logger.info("Master exited.")
  }

}
