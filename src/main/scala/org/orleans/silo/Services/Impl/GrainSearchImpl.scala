package org.orleans.silo.Services.Impl

import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Client.{ActivateGrainClient, ServiceFactory}
import org.orleans.silo.Services.Service
import org.orleans.silo.grainSearch.{
  GrainSearchGrpc,
  SearchRequest,
  SearchResult
}
import org.orleans.silo.utils.{GrainDescriptor, GrainState, SlaveDetails}

import scala.concurrent.Future

/**
  * Implementation of the searchGrain service. The service is binded on the gRPC server
  * and searchGrain can be called through remote call.
  */
//TODO Make the grainMap ConcurrentHashMap[String, List(GrainDescriptor)]
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
      val grain: GrainDescriptor = grainMap.get(id)
      //TODO Handle the List of GrainDescriptors if grain is present on multiple slaves
      val slaveDetails: SlaveDetails = grain.location
      var reply: SearchResult = SearchResult()
      // Check if the grain is active
      if (GrainState.InMemory.equals(grain.state)) {
        // Send the reply
        reply = SearchResult(serverAddress = slaveDetails.address,
                             serverPort = slaveDetails.port)
      } else {
        //Activate the grain and send the reply
        val client = ServiceFactory.activateGrainService(slaveDetails.address,
                                                         slaveDetails.port)
        //TODO Handle response from the slave
        client.activateGrain(id)
        reply = SearchResult(serverAddress = slaveDetails.address,
                             serverPort = slaveDetails.port)
      }
      Future.successful(reply)
    } else {
      logger.debug("Grain doesn't exists in the HashMap, returning failure")
      Future.failed(new Exception("Non existent grain Type"))
    }
  }
}
