package org.orleans.silo.runtime

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.lang.reflect.Method
import java.net.{ServerSocket, Socket}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors.newSingleThreadExecutor
import java.{lang, util}

import com.typesafe.scalalogging.LazyLogging
import io.grpc.{ServerBuilder, ServerServiceDefinition}
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.Test.GreeterGrain
import org.orleans.silo.utils.GrainState.GrainState
import org.orleans.silo.utils.{GrainState, ServerConfig}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

object Runtime{
  // Class that will serve as index for the grain map
  case class GrainInfo(port: Int,
                       state:GrainState,
                       grain: Grain,
                       grainType: String,
                       grainPackage: String,
                       load: Int)

  val GRPC_SUFFIX = "Grpc"
  val GRPC_SUBCLASS_SUFFIX = "Grpc$"
  val SERVICE_BINDER = "bindService"
}




/**
 * Server runtime
 * - Keep the information of the grains and their information
 * - Keep track of the state of the grains periodically to deactivate/ replicate
 * - Manage the communications with other servers' runtime
 *
 * (run is to test that just one slave sends the grain to the other slave)
 *
 */
class Runtime(val config: ServerConfig, id: String, report: Boolean)
    extends Runnable
    with LazyLogging{

  import Runtime._

  // Hashmap that will save the current grain information, indexed by port
  val grainMap : ConcurrentHashMap[String, GrainInfo] = new ConcurrentHashMap[String, GrainInfo]()

  // Free ports that the Server will manage to give to Grains
  // We keep a concurrent hashset so the elements can be added and removed safely
  val freePorts: ConcurrentHashMap.KeySetView[Int, lang.Boolean] = ConcurrentHashMap.newKeySet()
  private val ports = (5000 to 20000).toList
  // Have to do this to add multiple at one (a bit hacky but the only way)
  freePorts.addAll(new util.ArrayList[Int](ports.asJava))

  // Replication listener
  var replThread : Thread = _
  if (id != "master") {
    replThread = new Thread(new ReplicationListener)
    replThread.setName(s"REPL-LISTENER-$id")
  }

  /**
   * Get the head of the ports and return it
   * @return an unused port
   */
  def getFreePort: Int = {
    val port : Int = freePorts.toArray().head.asInstanceOf[Int]
    logger.info(s"removing port $port")
    freePorts.remove(port)
    port
  }


  /**
   * This should check the grains and check the load of them, in order to
   * keep the load balanced.
   * - If a grain is idle for a long time, store it
   * - If a grain is too loaded, initiate the replication
   */
  override def run(): Unit = {
    //grainMap.put("1234", GrainInfo(1, GrainState.InMemory, new GreeterGrain("1234"), 0))
    logger.info("Runtime running")

    // Start a listener thread that will look for new grains
    // being set to the slave
    if (id != "master")
      replThread.start()
    var sent = false

    // Test to make just one slave send stuff
    if(report) {
      // Infinite loop to check the state of grains
      while (true) {
        /* TODO Right now just do that for each grain that it finds sends
       a replication request, we should look at the load first */
        if (grainMap.isEmpty || sent)
          Thread.sleep(500)
        else {
          // Try to send the grain info
          grainMap.forEach((k, info) => {
            logger.info(s"Getting grain with key $k and info $info")
            logger.info(s"Now sending $info")
            // We know that the other slave is listening in port 2001
            val replicationSocket : Socket = new Socket("localhost", 2001)
            // Get the stream from which we'll send the grain
            val grainStream : ObjectOutputStream = new ObjectOutputStream(replicationSocket.getOutputStream)
            grainStream.writeObject(info)
            logger.info(s"Grain sent to other slave")
            sent=true
          })
        }


      }
    }

  }

  /**
   * Thread that will be listening in case of
   */
  private[Runtime] class ReplicationListener
      extends Runnable
      with LazyLogging {


    // Port to listen for replication requests
    // TODO right now it's random to run more than one in localhost
    private val REPL_PORT = if(report) 2000 else 2001
    logger.info(s"Trying to establish repl_port in port $REPL_PORT")
    private val replicationSocket : ServerSocket = new ServerSocket(REPL_PORT)

    /**
     * Wait in a socket to receive a grain to replicate.
     *
     * Right now we'll do everything without spamming new Threads, cause just creating a server
     * is not that costly
     */
    override def run(): Unit = {
      logger.info(s"Slave replication listener waiting in port $REPL_PORT")

      // Start the replication socket
      while(true){
        val requestSocket : Socket = replicationSocket.accept()
        logger.info(s"Got request from ${requestSocket.getInetAddress}:${requestSocket.getPort}")

        // Get the input stream
        val input : ObjectInputStream = new ObjectInputStream(requestSocket.getInputStream)
        // We know that what we're gonna get is a grain
        val info : GrainInfo = input.readObject().asInstanceOf[GrainInfo]
        logger.info(s"Got grain ${info.grain} of type ${info.grainType}")


        val definition = getServiceDefinition(info)
        // Trying to create a new server for that
        val port = 5001
        ServerBuilder
          .forPort(port)
          .addService(definition)
          .build
          .start
      }
    }

    private[this] def getServiceDefinition(info: GrainInfo) : ServerServiceDefinition = {
      val serviceDefinitionClass: Class[_] = Class
        .forName(info.grainPackage + "." + info.grainType + GRPC_SUFFIX)

      // Get the actual interface for that object
      val serviceInterface: Class[_] = Class
        .forName(
          info.grainPackage + "." + info.grainType + GRPC_SUBCLASS_SUFFIX + info.grainType)

      // Get the bindService method
      val binder: Method = serviceDefinitionClass
        .getDeclaredMethod(SERVICE_BINDER,
          serviceInterface,
          classOf[ExecutionContext])
      binder.setAccessible(true)


      // Now create the ServerServiceDefinition with the grain object!
      val ssd: ServerServiceDefinition = binder
        .invoke(null,
          info.grain.asInstanceOf[Object],
          ExecutionContext
            .fromExecutorService(newSingleThreadExecutor)
            .asInstanceOf[Object])
        .asInstanceOf[ServerServiceDefinition]

      ssd
    }
  }


}
