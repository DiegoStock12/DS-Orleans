package org.orleans.silo.Services.Impl

import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.grainSearch.{
  GrainSearchGrpc,
  SearchRequest,
  SearchResult
}
import org.orleans.silo.utils.{GrainDescriptor, SlaveDetails}

import scala.collection.mutable
import scala.concurrent.Future

class GrainSearchImpl(val grainMap: ConcurrentHashMap[String, GrainDescriptor])
    extends GrainSearchGrpc.GrainSearch
    with LazyLogging {

  logger.debug("Created the class with the map ")
  grainMap.forEach((k, v) => logger.debug(k + ":" + v))

  override def searchGrain(request: SearchRequest): Future[SearchResult] = {
    val id = request.grainID
    logger.debug("Client is looking for grain " + id)
    grainMap.forEach((k, v) => logger.debug(k + " -> " + v))
    if (grainMap.containsKey(id)) {
      logger.debug("Grain exists in the HashMap, returning success")
      // Get the details from the server and send the reply
      val slaveDetails: SlaveDetails = grainMap.get(id).location
      val reply = SearchResult(serverAddress = slaveDetails.address,
                               serverPort = slaveDetails.port)
      Future.successful(reply)
    } else {
      logger.debug("Grain doesn't exists in the HashMap, returning failure")
      Future.failed(new Exception("Non existent grain Type"))
    }
  }
}
