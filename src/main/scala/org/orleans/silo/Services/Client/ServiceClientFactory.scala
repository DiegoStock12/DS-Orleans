package org.orleans.silo.Services.Client

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.orleans.silo.Services.Client.ServiceClientFactory.{channel, searchStub}
import org.orleans.silo.Services.Service
import org.orleans.silo.Services.Service.Service
import org.orleans.silo.activateGrain.ActivateGrainServiceGrpc
import org.orleans.silo.grainSearch.{GrainSearchGrpc, SearchResult}
import org.orleans.silo.hello.GreeterGrpc
import scala.concurrent.{Await, Future}

import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Factory for getting the client to particular service
 */
object ServiceClientFactory {
  // Apply with default port for the master
  private var searchStub: SearchServiceClient = _
  private var channel: ManagedChannel = _

  def apply(master: String, port: Int = 50050): ServiceClientFactory = {
    channel = ManagedChannelBuilder.forAddress(master, port).usePlaintext().build()
    searchStub = new SearchServiceClient(channel, GrainSearchGrpc.stub(channel))
    new ServiceClientFactory()
  }

}

class ServiceClientFactory() {

  /**
   * Gets the grain reference
   *
   * It does everything in one go:
   * - Asks the master for a specific grain
   * - The master answers eventually with the address of the grain (server:port)
   * - Builds the stub so the user can just use it
   *
   * @param service service type from the defined ones
   * @param id      id of the service
   */
  def getGrain(service: Service, id: String): Future[ServiceClient] = {
    val f: Future[SearchResult] = searchStub.search(id)
    f.map(serverDetails => {
      val c = ManagedChannelBuilder.forAddress(serverDetails.serverAddress, serverDetails.serverPort).usePlaintext().build()
      service match {
        //TODO maybe we could have a map of services to clients so it's more automated
        case Service.Hello =>
          val stub = GreeterGrpc.stub(c)
          new GreeterClient(c, stub)
        case Service.ActivateGrain =>
          val stub = ActivateGrainServiceGrpc.stub(channel)
          new ActivateGrainClient(channel, stub)
      }
    })

  }


}


