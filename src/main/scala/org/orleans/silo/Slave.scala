package org.orleans.silo

import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}
import org.orleans.silo.Services.UpdateStateServiceImpl
import org.orleans.silo.updateGrainState.UpdateGrainStateServiceGrpc
import org.orleans.silo.utils.GrainDescriptor

import scala.collection.mutable
import scala.concurrent.ExecutionContext

object Slave  {
  // logger for the classes
  private val logger = Logger.getLogger(classOf[Slave].getName)
  private val port = 50060
  private val address = "10.100.9.99"

  def start(): Unit = {
    val slave = new Slave(ExecutionContext.global)
    slave.start()
    slave.blockUntilShutdown()
  }
}


//TODO I think we should run gRPC server for receiving request in other thread.
class Slave(executionContext: ExecutionContext) extends Runnable{

  // For now just define it as a gRPC endpoint
  self =>
  private[this] var slave: Server = null
  // Hashmap to save the grain references
  private val grainMap: mutable.HashMap[String, GrainDescriptor] = mutable.HashMap[String, GrainDescriptor]()


  /**
   * Start the gRPC server for GrainLookup
   */
  private def start(): Unit = {
    slave = ServerBuilder.forPort(Slave.port).addService(UpdateGrainStateServiceGrpc.bindService(new UpdateStateServiceImpl(), executionContext))
      .build.start

    Slave.logger.info("Slave server started, listening on port " + Slave.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  def stop(): Unit = {
    if (slave != null) {
      slave.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (slave != null) {
      slave.awaitTermination()
    }
  }

  def run {
    // Code here
  }
}
