package org.orleans.silo.Services.Client

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging

import io.grpc.ManagedChannel

import org.orleans.silo.grainSearch.GrainSearchGrpc.GrainSearchStub
import org.orleans.silo.grainSearch.{SearchRequest, SearchResult}

import scala.concurrent.Future

/**
  * Class that you can use to execute searchGrain service on a remote server through gRPC call.
  */
class SearchServiceClient(val channel: ManagedChannel)
    extends ServiceClient(channel, new GrainSearchStub(channel))
    with LazyLogging {

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
