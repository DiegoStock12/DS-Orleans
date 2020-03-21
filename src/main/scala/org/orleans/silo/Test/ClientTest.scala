package org.orleans.silo.Test

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.orleans.silo.Services.Client.{SearchServiceClient, ServiceFactory}
import org.orleans.silo.Services.Service
import org.orleans.silo.grainSearch.GrainSearchGrpc.GrainSearchStub
import org.orleans.silo.grainSearch.{GrainSearchGrpc, SearchRequest, SearchResult}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ClientTest {
  def apply(host: String, port: Int): ClientTest = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    val stub = GrainSearchGrpc.stub(channel)
    new ClientTest(channel, stub)
  }

  def main(args: Array[String]): Unit = {
//    val client = ClientTest("localhost", 50050)
    val client = ServiceFactory.getService(Service.GrainSearch, "localhost", 50050).asInstanceOf[SearchServiceClient]
    try {
      client.search("diegoalbo")
    }
    finally {
      client.shutdown()
    }

  }
}

class ClientTest (val channel: ManagedChannel, val stub: GrainSearchStub) {
  private[this] val logger = Logger.getLogger(classOf[ClientTest].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.MILLISECONDS)
  }

  def search(id: String): Unit = {
    logger.info("Will try to find the address of the grain User/" + id)
    val request = SearchRequest(grainID =  id)
    println(request)
    try {
      val f : Future[SearchResult] = stub.searchGrain(request)
      f onComplete {
        case Success(results) => println("Search Results -> Address "+ results.serverAddress+", port: "+results.serverPort)
        case Failure(exception) => exception.printStackTrace()
      }

      Thread.sleep(10000)
    }

  }

}
