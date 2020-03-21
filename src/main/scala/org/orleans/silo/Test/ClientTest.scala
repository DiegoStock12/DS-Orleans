package org.orleans.silo.Test

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.orleans.silo.grainSearch.GrainSearchGrpc.GrainSearchStub
import org.orleans.silo.grainSearch.{GrainSearchGrpc, SearchRequest, SearchResult}
import org.orleans.silo.updateGrainState.UpdateGrainStateServiceGrpc.UpdateGrainStateServiceStub
import org.orleans.silo.updateGrainState.{UpdateGrainStateServiceGrpc, UpdateStateRequest, UpdateSuccess}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ClientTest {
  def apply(host: String, port: Int): ClientTest = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    val searchStub = GrainSearchGrpc.stub(channel)
    val updateStub = UpdateGrainStateServiceGrpc.stub(channel)
    new ClientTest(channel, searchStub, updateStub)
  }

  def main(args: Array[String]): Unit = {
    val client = ClientTest("localhost", 50050)
    try {
      client.updateState("diegoalbo", "InMemory", "localhost")
      client.search("diegoalbo")
    }
    finally {
      client.shutdown()
    }

  }
}

class ClientTest(val channel: ManagedChannel, val searchStub: GrainSearchStub, updateStub: UpdateGrainStateServiceStub) {
  private[this] val logger = Logger.getLogger(classOf[ClientTest].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.MILLISECONDS)
  }

  def search(id: String): Unit = {
    logger.info("Will try to find the address of the grain User/" + id)
    val request = SearchRequest(grainID = id)
    println(request)
    try {
      val f: Future[SearchResult] = searchStub.searchGrain(request)
      f onComplete {
        case Success(results) => println("Search Results -> Address " + results.serverAddress + ", port: " + results.serverPort)
        case Failure(exception) => exception.printStackTrace()
      }

      Thread.sleep(5000)
    }
  }

  def updateState(id: String, state: String, source: String): Unit = {
    logger.info("Will try to update the state of the grain " + id)
    val request = UpdateStateRequest(id, state, source)
    println(request)
    try {
      val f: Future[UpdateSuccess] = updateStub.updateState(request)
      f onComplete {
        case Success(result) => println("Grain state update: " + result.success)
        case Failure(exception) => exception.printStackTrace()
      }

      Thread.sleep(5000)
    }

  }

}
