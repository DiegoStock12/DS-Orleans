package org.orleans.silo.Services.Client

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import io.grpc.ManagedChannel
import org.orleans.silo.grainSearch.GrainSearchGrpc.GrainSearchStub
import org.orleans.silo.grainSearch.{SearchRequest, SearchResult}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class SearchServiceClient(val channel: ManagedChannel,
                          val stub: GrainSearchStub)
    extends ServiceClient
    with LazyLogging {

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.MILLISECONDS)
  }

  // Returns a future so it's more async
  def search(id: String): Future[SearchResult] = {
    logger.info("Will try to find the address of the grain " + id)
    val request = SearchRequest(grainID = id)
    println(request)
    try {
      val f: Future[SearchResult] = stub.searchGrain(request)
      println("Returning a future " + f)
      f
    }
  }

}
