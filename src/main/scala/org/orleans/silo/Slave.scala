package org.orleans.silo
import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.grpc.{Server, ServerBuilder}
import org.orleans.silo.Services.Impl.{ActivateGrainImpl, CreateGrainImpl}
import org.orleans.silo.Test.GreeterGrain
import org.orleans.silo.activateGrain.ActivateGrainServiceGrpc
import org.orleans.silo.communication.ConnectionProtocol._
import org.orleans.silo.communication.{PacketListener, PacketManager, ConnectionProtocol => protocol}
import org.orleans.silo.createGrain.CreateGrainGrpc
import org.orleans.silo.dispatcher.Dispatcher
import org.orleans.silo.runtime.Runtime
import org.orleans.silo.utils.{GrainDescriptor, ServerConfig}

import scala.collection.mutable
import scala.concurrent.ExecutionContext


/**
 * Slave silo, handles request from the master
 * @param slaveConfig Server config for the slave
 * @param masterConfig Config of the master server
 * @param executionContext Execution context for the RPC services
 */
class Slave(slaveConfig: ServerConfig,
            masterConfig: ServerConfig,
            executionContext: ExecutionContext,
            report : Boolean)
    extends LazyLogging
    with Runnable
    with PacketListener {

  // For now just define it as a gRPC endpoint
  self =>
  private[this] var slave: Server = null

  // Hashmap to save the grain references
  private val grainMap: mutable.HashMap[String, GrainDescriptor] =
    mutable.HashMap[String, GrainDescriptor]()

  // Metadata for the slave.
  val uuid: String = UUID.randomUUID().toString
  val shortId: String = protocol.shortUUID(uuid)

  @volatile
  var running: Boolean = false
  val SLEEP_TIME: Int = 100

  // Master information.
  @volatile
  var connectedToMaster: Boolean = false
  @volatile
  var masterInfo: MasterInfo = MasterInfo("", 0)

  // Packetmanager which send packets and receives packets (event-driven).
  val packetManager: PacketManager =
    new PacketManager(this, slaveConfig.udpPort)

  // Hash table of other slaves. This is threadsafe.
  val slaves = scala.collection.mutable.HashMap[String, SlaveInfo]()

  // Runtime object that keeps track of grain activity
  val runtime : Runtime = new Runtime(slaveConfig, protocol.shortUUID(uuid), report = report)

  val dispatcher = new Dispatcher(new GreeterGrain("1234"), 2500)


  /**
    * Starts the slave.
    * - Creates a main control loop to send information to the master.
    * - Creates a packet-manager which handles incoming and outgoing packets.
    */
  def start() = {
    logger.info(f"Now starting slave with id: ${protocol.shortUUID(uuid)}.")
    this.running = true

    // Starting a packet manager which listens for incoming packets.
    packetManager.init(shortId)

    // Creating slave thread and starting it.
    val slaveThread = new Thread(this)
    slaveThread.setName(f"slave-$shortId")
    slaveThread.start()

    // Start runtime thread
    val runtimeThread = new Thread(runtime)
    runtimeThread.setName("runtime")
    //runtimeThread.start()

    val dispatcherThread = new Thread(dispatcher)
    dispatcherThread.setName("Dispatcher")
    dispatcherThread.start()

    startgRPC()

  }

  /**
    * Starts the gRPC server.
    */
  private def startgRPC() = {
    slave = ServerBuilder
      .forPort(slaveConfig.rpcPort)
      .addService(
        ActivateGrainServiceGrpc.bindService(new ActivateGrainImpl(),
                                             executionContext))
        .addService(
          CreateGrainGrpc.bindService(new CreateGrainImpl("slave", runtime),
            executionContext))
      .build
      .start

//    ServerBuilder
//      .forPort(50400)
//      .addService(GreeterGrpc.bindService(new GreeterImpl(), executionContext))
//      .build
//      .start

    logger.info(
      "Slave server started, listening on port " + slaveConfig.rpcPort)
    sys.addShutdownHook {
      logger.error("*** shutting down gRPC server since JVM is shutting down")
      // TODO if we're gonna have more services we should get a list of services so we can shut them down correctly
      this.stop()
    }
  }

  /** Control loop. */
  def run(): Unit = {
    var oldTime: Long = System.currentTimeMillis()

    while (running) {
      // If not connected to the master, lets do a handshake.
      if (!connectedToMaster) {
        if (!connectToMaster()) return // If we can't connect, exit this loop.
      }

      // Keep track of local time, to ensure sending heartbeats on time.
      val newTime: Long = System.currentTimeMillis()
      val timeDiff = newTime - oldTime

      // Check if it is time to send heartbeats again.
      if (timeDiff >= protocol.heartbeatInterval) {
        logger.debug("Sending heartbeats to master.")

        // Send heartbeat to the master.
        val heartbeat = Packet(PacketType.HEARTBEAT, this.uuid, newTime)
        packetManager.send(heartbeat, masterConfig.host, masterConfig.udpPort)

        // Update time
        oldTime = newTime
      }

      verifyMasterAlive()

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
  def connectToMaster(): Boolean = {
    logger.info("Connecting to master.")
    for (i <- 1 to protocol.connectionAttempts) {
      // Send a handshake and wait for a bit.
      val handshake =
        new Packet(PacketType.HANDSHAKE, this.uuid, System.currentTimeMillis())
      packetManager.send(handshake, masterConfig.host, masterConfig.udpPort)
      Thread.sleep(protocol.connectionDelay)

      // If connected, the slave can start its life.
      if (connectedToMaster) {
        return true
      }

      logger.info(
        s"Couldn't connect to master. Attempt $i/${protocol.connectionAttempts}.")
    }

    // If couldn't connect to the master after x attempts, shutdown server.
    logger.error("Couldn't connect to master. Now shutting down.")
    this.stop()

    // We couldn't connect at all.
    return false
  }

  /**
    * Verifies if the master is still alive. If not, the slave gets disconnected (and then tries to reconnect).
    */
  def verifyMasterAlive(): Unit = {
    val diffTime = System.currentTimeMillis() - masterInfo.lastHeartbeat
    if (diffTime >= protocol.deathTime) {
      //consider it dead
      logger.warn("Connection to master timed out. Will try reconnect.")
      masterInfo = MasterInfo("", -1)
      connectedToMaster = false
    }
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
    case PacketType.WELCOME       => processWelcome(packet, host, port)
    case PacketType.HEARTBEAT     => processHeartbeat(packet, host, port)
    case PacketType.SLAVE_CONNECT => processSlaveConnect(packet, host, port)
    case PacketType.SLAVE_DISCONNECT =>
      processSlaveDisconnect(packet, host, port)
    case PacketType.SHUTDOWN => processShutdown(packet, host, port)
    case _                   => logger.error(s"Did not expect this packet: $packet.")

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
    * Processes connection of a new slave.
    * 1). Add slave to local table (if not already there).
    *
    * @param packet the connect packet.
    * @param host the host receiving from.
    * @param port the port receiving from.
    */
  def processSlaveConnect(packet: Packet, host: String, port: Int): Unit = {
    // If slave is already in the cluster, we will not add it again.
    if (slaves.contains(packet.uuid)) return

    // Store slaveInfo from data in packet.
    slaves.put(packet.uuid,
               SlaveInfo(packet.uuid, packet.data(0), packet.data(1).toInt))
    logger.debug(
      s"Added new slave ${protocol.shortUUID(packet.uuid)} to local hashtable.")
  }

  /**
    * Processes disconnect of a new slave.
    * 1). Remove slave from local table (if there).
    *
    * @param packet the disconnect packet.
    * @param host the host receiving from.
    * @param port the port receiving from.
    */
  def processSlaveDisconnect(packet: Packet, host: String, port: Int): Unit = {
    // If slave is not in the cluster, we will ignore this packet.
    if (!slaves.contains(packet.uuid)) return

    // Store slaveInfo from data in packet.
    slaves.remove(packet.uuid)
    logger.debug(
      s"Removed slave ${protocol.shortUUID(packet.uuid)} from local hashtable.")
  }

  /**
    * Processes a shutdown packet by stopping the slave.
    * @param packet the shutdown packet.
    * @param host the host receiving from.
    * @param port the port receiving from.
    */
  def processShutdown(packet: Packet, host: String, port: Int): Unit = {
    // Check if it is actually the master.
    if (packet.uuid != masterInfo.uuid) {
      logger.warn(
        "Got a shutdown packet from a source which doesn't seem to be the master. Ignoring it.")
      return
    }

    // Stops the slave.
    this.stop()
  }

  /** Returns all slaves. **/
  def getSlaves(): List[SlaveInfo] = slaves.toList.map(_._2)

  /**
    * Stopping the slave.
    * Returns if it isn't running.
    */
  def stop(): Unit = {
    if (!running) return
    logger.info(f"Now stopping slave with id: ${protocol.shortUUID(uuid)}.")

    // Send shutdown packet to master.
    val shutdown =
      Packet(PacketType.SHUTDOWN, this.uuid, System.currentTimeMillis())
    packetManager.send(shutdown, masterConfig.host, masterConfig.udpPort)

    // cancel itself
    if (slave != null) {
      slave.shutdown()
    }
    this.packetManager.cancel()
    this.running = false
    this.slaves.clear()
    logger.info("Slave exited.")
  }
}
