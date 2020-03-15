package org.orleans.silo.Services.Impl

import org.orleans.silo.grainSearch.{GrainSearchGrpc, SearchRequest, SearchResult}
import org.orleans.silo.utils.{GrainDescriptor, SlaveDetails}

import scala.collection.mutable
import scala.concurrent.Future

class GrainSearchImpl(val grainMap: mutable.HashMap[String, GrainDescriptor]) extends GrainSearchGrpc.GrainSearch {

  print("Created the class with the map ")
  grainMap.foreach(println)

  override def searchGrain(request: SearchRequest): Future[SearchResult] = {
    val gtype = request.grainType
    println("Client is looking for grain type " + gtype)
    if (grainMap.contains(gtype)) {
      println("Grain exists in the HashMap, returning success")
      // Get the details from the server and send the reply
      val slaveDetails: SlaveDetails = grainMap(gtype).location
      val reply = SearchResult(serverAddress = slaveDetails.address, serverPort = slaveDetails.port)
      Future.successful(reply)
    } else {
      println("Grain doesn't exists in the HashMap, returning failure")
      Future.failed(new Exception("Non existant grain Type"))
    }
  }
}
