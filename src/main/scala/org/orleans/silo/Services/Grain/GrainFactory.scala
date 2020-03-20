package org.orleans.silo.Services.Grain

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.orleans.silo.Services.Client.SearchServiceClient
import org.orleans.silo.grainSearch.{GrainSearchGrpc, SearchResult}
import org.orleans.silo.Services.Grain.GrainFactory.{channel, searchStub}

import scala.concurrent.Future


/**
 * Factory for obtaining the grains
 */
object GrainFactory {
  // Apply with default port for the master
  private var searchStub: SearchServiceClient = _
  private var channel: ManagedChannel = _

  def apply(master: String, port: Int = 50050): GrainFactory = {
    channel = ManagedChannelBuilder.forAddress(master, port).usePlaintext().build()
    searchStub = new SearchServiceClient(channel, GrainSearchGrpc.stub(channel))
    new GrainFactory()
  }
}

class GrainFactory() {

  /**
   * Gets the grain reference
   *
   * It does everything in one go:
   * - Asks the master for a specific grain
   * - The master answers eventually with the address of the grain (server:port)
   * - Builds the stub so the user can just use it
   *
   * @param id      id of the service
   */
  def getGrain(id: String): Future[SearchResult] = {
    val f: Future[SearchResult] = searchStub.search(id)
    f
  }

  def createGrain(): Unit = {
    //TODO
  }

  def deleteGrain(): Unit = {
    //TODO
  }

}


