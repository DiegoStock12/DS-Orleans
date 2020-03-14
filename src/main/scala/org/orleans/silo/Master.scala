package org.orleans.silo

import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}
import org.orleans.silo.grainSearch.GrainSearchGrpc

import scala.concurrent.ExecutionContext
import org.orleans.silo.Services.{ActivateGrainImpl, GrainSearchImpl}
import org.orleans.silo.activateGrain.ActivateGrainServiceGrpc
import org.orleans.silo.utils.{GrainDescriptor, GrainState, SlaveDetails}

import scala.collection.mutable


object Master  {
  // logger for the classes
  private val logger = Logger.getLogger(classOf[Master].getName)
  private val port = 50050

   def start(): Unit = {
    val server = new Master(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }
}

//TODO I think we should run gRPC server for receiving request in other thread.
class Master(executionContext: ExecutionContext) extends Runnable{
  // For now just define it as a gRPC endpoint
  self =>
  private[this] var master: Server = null
  // Hashmap to save the grain references
  private val grainMap: mutable.HashMap[String, GrainDescriptor] = mutable.HashMap[String, GrainDescriptor]()
  // Add a default object


  /**
   * Start the gRPC server for GrainLookup
   */
  private def start(): Unit = {
    grainMap += "User" -> GrainDescriptor(GrainState.Activating, SlaveDetails("10.100.5.6", 5640))
    master = ServerBuilder.forPort(Master.port).addService(GrainSearchGrpc.bindService(new GrainSearchImpl(grainMap), executionContext))
      .addService(ActivateGrainServiceGrpc.bindService(new ActivateGrainImpl, executionContext)).build.start

    Master.logger.info("Master server started, listening on port " + Master.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  def stop(): Unit = {
    if (master != null) {
      master.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (master != null) {
      master.awaitTermination()
    }
  }

  def run {
    // Code here
  }
}
