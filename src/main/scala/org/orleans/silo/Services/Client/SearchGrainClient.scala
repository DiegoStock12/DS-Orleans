package org.orleans.silo.Services.Client

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import scala.concurrent.ExecutionContext.Implicits.global

import io.grpc.ManagedChannel
import org.orleans.silo.grainSearch.GrainSearchGrpc.GrainSearchStub
import org.orleans.silo.grainSearch.{SearchRequest, SearchResult}

import scala.concurrent.Future
import scala.util.{Failure, Success}


class SearchGrainClient (val channel: ManagedChannel, val stub: GrainSearchStub) extends ServiceClient {
  private[this] val logger = Logger.getLogger(classOf[SearchGrainClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.MILLISECONDS)
  }

  def search(id: String): Unit = {
    logger.info("Will try to find the address of the grain User/" + id)
    val request = SearchRequest(grainType =  "Tonto", grainID =  id)
    println(request)
    try {
      val f : Future[SearchResult] = stub.searchGrain(request)
      f onComplete {
        case Success(results) => println(results)
        case Failure(exception) => exception.printStackTrace()
      }

      Thread.sleep(10000)
    }
  }

}