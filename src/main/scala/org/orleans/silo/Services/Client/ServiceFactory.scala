package org.orleans.silo.Services.Client

import io.grpc.ManagedChannelBuilder
import org.orleans.silo.Services.Service
import org.orleans.silo.Services.Service.Service
import org.orleans.silo.Test.GreeterClient
import org.orleans.silo.activateGrain.ActivateGrainServiceGrpc
import org.orleans.silo.grainSearch.GrainSearchGrpc
import org.orleans.silo.hello.GreeterGrpc
import org.orleans.silo.updateGrainState.UpdateGrainStateServiceGrpc


/**
 * Factory for getting the client to particular service
 */
object ServiceFactory {

  /**
   * Gets the desired service
   *
   * @param service service type from the defined ones
   */
  def getService(service: Service, serverAddress: String, serverPort: Int): ServiceClient = {
    val c = ManagedChannelBuilder.forAddress(serverAddress, serverPort).usePlaintext().build()
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
      case Service.GrainStatusUpdate =>
        val stub = UpdateGrainStateServiceGrpc.stub(c)
        new UpdateGrainStateClient(c, stub)
    }
  }
}




