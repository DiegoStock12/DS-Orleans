package org.orleans.silo

import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}
import org.orleans.silo.Services.Impl.ActivateGrainImpl
import org.orleans.silo.activateGrain.ActivateGrainServiceGrpc
import org.orleans.silo.utils.GrainDescriptor

import scala.collection.mutable
import scala.concurrent.ExecutionContext

object Slave {
  // logger for the classes
  private val logger = Logger.getLogger(classOf[Slave].getName)
  private val port = 50060
  private val address = "10.100.9.99"

  def start(): Unit = {
    new Thread(new Slave(ExecutionContext.global)).start()
  }
}


class Slave(executionContext: ExecutionContext) extends Runnable {

  // For now just define it as a gRPC endpoint
  self =>
  private[this] var slave: Server = null
  // Hashmap to save the grain references
  private val grainMap: mutable.HashMap[String, GrainDescriptor] = mutable.HashMap[String, GrainDescriptor]()


  /**
   * Start the gRPC server for GrainLookup
   */
  private def start(): Unit = {
    slave = ServerBuilder.forPort(Slave.port).addService(ActivateGrainServiceGrpc.bindService(new ActivateGrainImpl(), executionContext))
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

  def run: Unit = {
    start()
    blockUntilShutdown()
  }
}
