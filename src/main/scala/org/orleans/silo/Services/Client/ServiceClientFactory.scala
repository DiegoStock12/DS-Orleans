package org.orleans.silo.Services.Client

import io.grpc.ManagedChannelBuilder
import org.orleans.silo.Services.Service
import org.orleans.silo.Services.Service.Service
import org.orleans.silo.activateGrain.ActivateGrainServiceGrpc
import org.orleans.silo.grainSearch.GrainSearchGrpc


/**
 * Factory for getting the client to particular service
 */
object ServiceClientFactory {
  def apply(service: Service, host: String, port : Int): ServiceClient = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()

    service match {
      case Service.ActivateGrain => {
        val stub = ActivateGrainServiceGrpc.stub(channel)
        new ActivateGrainClient(channel, stub)
      }
      case Service.GrainSearch => {
        val stub = GrainSearchGrpc.stub(channel)
        new SearchGrainClient(channel, stub)
      }
    }


  }

}
