package org.orleans.silo.Services.Impl

import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.LazyLogging
import main.scala.org.orleans.client.OrleansRuntime
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
class GrainSearchImpl(
    val grainMap: ConcurrentHashMap[String, List[GrainDescriptor]])
    extends GrainSearchGrpc.GrainSearch
    with LazyLogging {
  logger.debug("Created the class with the map ")
  grainMap.forEach((k, v) => logger.debug(k + ":" + v))

  override def searchGrain(request: SearchRequest): Future[SearchResult] = {
    val id = request.grainID
    logger.info("Client is looking for grain " + id)
    grainMap.forEach((k, v) => logger.info(k + " -> " + v))

    // First check if there grain was ever created
    if (grainMap.containsKey(id)) {
      logger.debug("Grain exists in the HashMap")
      var reply: SearchResult = SearchResult()

      val grains: List[GrainDescriptor] = grainMap.get(id)
      val activeGrains: List[GrainDescriptor] =
        grains.filter(grain => GrainState.InMemory.equals(grain.state))

      if (activeGrains.nonEmpty) {
        logger.debug("Found activated grain")
        // If there is slave where the grain is activated, talk to this node
        //TODO Add some logic for chosing the slave
        val chosenSlave: SlaveDetails = activeGrains.head.location
        reply = SearchResult(serverAddress = chosenSlave.address,
                             serverPort = chosenSlave.port)
        Future.successful(reply)
      } else {
        // Else choose some slave to activate the grain
        //TODO Add some logic for chosing the slave
        val chosenSlave: SlaveDetails = grains.head.location
        val client = ServiceFactory
          .getService[ActivateGrainClient](
            OrleansRuntime(chosenSlave.address, chosenSlave.port))
          .asInstanceOf[ActivateGrainClient]

        // Wait for the response. We want the activation request to be the blocking one.
        val activationSuccess: Boolean = client.activateGrain(id)
        if (activationSuccess) {
          reply = SearchResult(serverAddress = chosenSlave.address,
                               serverPort = chosenSlave.port)
          Future.successful(reply)
        } else {
          logger.debug("Grain failed to activate on node: {}",
                       chosenSlave.address)
          Future.failed(new Exception("Grain activation failure"))
        }
      }
    } else {
      logger.debug("Grain doesn't exists in the HashMap, returning failure")
      Future.failed(new Exception("Non existent grain Type"))
    }
  }
}
