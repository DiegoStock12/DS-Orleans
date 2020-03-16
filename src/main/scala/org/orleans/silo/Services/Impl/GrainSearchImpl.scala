package org.orleans.silo.Services.Impl

import java.util.concurrent.ConcurrentHashMap

import org.orleans.silo.grainSearch.{GrainSearchGrpc, SearchRequest, SearchResult}
import org.orleans.silo.utils.{GrainDescriptor, SlaveDetails}

import scala.collection.mutable
import scala.concurrent.Future

class GrainSearchImpl(val grainMap: ConcurrentHashMap[String, GrainDescriptor]) extends GrainSearchGrpc.GrainSearch {

  print("Created the class with the map ")
  grainMap.forEach((k, v) => println(k+":"+ v))

  override def searchGrain(request: SearchRequest): Future[SearchResult] = {
    val id = request.grainID
    println("Client is looking for grain " + id)
    grainMap.forEach((k, v) => println(k+" -> "+ v))
    if (grainMap.containsKey(id)) {
      println("Grain exists in the HashMap, returning success")
      // Get the details from the server and send the reply
      val slaveDetails: SlaveDetails = grainMap.get(id).location
      val reply = SearchResult(serverAddress = slaveDetails.address, serverPort = slaveDetails.port)
      Future.successful(reply)
    } else {
      println("Grain doesn't exists in the HashMap, returning failure")
      Future.failed(new Exception("Non existant grain Type"))
    }
  }
}
