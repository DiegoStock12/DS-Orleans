package org.orleans.silo

import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}
import org.orleans.silo.grainSearch.{GrainSearchGrpc, SearchRequest, SearchResult}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}


object Master  {
  // logger for the classes
  private val logger = Logger.getLogger(classOf[Master].getName)
  private val port = 50050
  private val address = "10.100.9.99"

   def start(): Unit = {
    val server = new Master(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }
}

private case class SlaveDetails(address: String, port: Int)

class Master(executionContext: ExecutionContext) {
  // For now just define it as a gRPC endpoint
  self =>
  private[this] var server: Server = null
  // Hashmap to save the grain references
  private val grainMap: mutable.HashMap[String, SlaveDetails] = mutable.HashMap[String, SlaveDetails]()
  // Add a default object


  /**
   * Start the gRPC server for GrainLookup
   */
  private def start(): Unit = {
    grainMap += "User" -> new SlaveDetails("10.100.5.6", 5640)
    server = ServerBuilder.forPort(Master.port).addService(GrainSearchGrpc.bindService(new GrainSearchImpl(grainMap), executionContext)).build.start
    Master.logger.info("Master server started, listening on port " + Master.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

}

private class GrainSearchImpl(val grainMap: mutable.HashMap[String, SlaveDetails]) extends GrainSearchGrpc.GrainSearch {

  print("Created the class with the map ")
  grainMap.foreach(println)

  override def searchGrain(request: SearchRequest): Future[SearchResult] = {
    val gtype = request.grainType
    println("Client is looking for grain type " + gtype)
    if (grainMap.contains(gtype)) {
      println("Grain exists in the HashMap, returning success")
      // Get the details from the server and send the reply
      val slaveDetails: SlaveDetails = grainMap(gtype)
      val reply = SearchResult(serverAddress = slaveDetails.address, serverPort = slaveDetails.port)
      Future.successful(reply)
    } else {
      println("Grain doesn't exists in the HashMap, returning failure")
      Future.failed(new Exception("Non existant grain Type"))
    }
  }
}
