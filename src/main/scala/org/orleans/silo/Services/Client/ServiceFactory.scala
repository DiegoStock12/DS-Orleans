package org.orleans.silo.Services.Client

import io.grpc.ManagedChannelBuilder
import org.orleans.silo.Services.Service
import org.orleans.silo.Services.Service.Service
import org.orleans.silo.Test.GreeterClient
import org.orleans.silo.activateGrain.ActivateGrainServiceGrpc
import org.orleans.silo.createGrain.CreateGrainGrpc
import org.orleans.silo.grainSearch.GrainSearchGrpc
import org.orleans.silo.hello.GreeterGrpc

/**
  * Factory for getting the client to particular service
  */
object ServiceFactory {

  /**
    * Gets the desired service
    *
    * @param service service type from the defined ones
    */
  def getService(service: Service,
                 serverAddress: String,
                 serverPort: Int,
                 stubType: String = "async"): ServiceClient = {
    val c = ManagedChannelBuilder
      .forAddress(serverAddress, serverPort)
      .usePlaintext()
      .build()
    service match {
      case Service.Hello =>
        val stub = GreeterGrpc.stub(c)
        new GreeterClient(c, stub)
      case Service.ActivateGrain =>
        val stub = ActivateGrainServiceGrpc.stub(c)
        new ActivateGrainClient(c, stub)
      case Service.GrainSearch =>
        val stub = GrainSearchGrpc.stub(c)
        new SearchServiceClient(c, stub)
      case Service.CreateGrain =>
        new CreateGrainClient(c, stubType)
    }
  }

  def createGrainService(serverAddress: String,
                         serverPort: Int,
                         stubType: String = "async"): CreateGrainClient =
    getService(Service.CreateGrain, serverAddress, serverPort, stubType)
      .asInstanceOf[CreateGrainClient]

  def searchGrainService(serverAddress: String,
                         serverPort: Int,
                         stubType: String = "async"): SearchServiceClient =
    getService(Service.GrainSearch, serverAddress, serverPort, stubType)
      .asInstanceOf[SearchServiceClient]

  def activateGrainService(serverAddress: String,
                           serverPort: Int,
                           stubType: String = "async"): ActivateGrainClient =
    getService(Service.ActivateGrain, serverAddress, serverPort, stubType)
      .asInstanceOf[ActivateGrainClient]
}
