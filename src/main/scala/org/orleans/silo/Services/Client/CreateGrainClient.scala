package org.orleans.silo.Services.Client

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging
import io.grpc.ManagedChannel
import org.orleans.silo.Services.Service
import org.orleans.silo.createGrain.CreateGrainGrpc.{CreateGrainBlockingStub, CreateGrainStub}
import org.orleans.silo.createGrain.{CreateGrainGrpc, CreationRequest, CreationResponse}
import org.orleans.silo.grainSearch.{SearchRequest, SearchResult}

import scala.concurrent.Future

class CreateGrainClient(val channel: ManagedChannel,
                        val stubType: String = "async")
  extends ServiceClient
    with LazyLogging {

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.MILLISECONDS)
  }

  // Returns a future so it's more async
  def createGrain(serviceId: Int, serviceName: String): Future[CreationResponse] = {
    logger.info("Sending request to create grain of service " + Service.values.toList(serviceId))
    logger.info("Sending service implementation "+serviceName)
    val request = CreationRequest(service = Service.CreateGrain.id,
      serviceDefinition = serviceName)
    println(request)

    stubType match {
      case "sync" =>
        val stub = CreateGrainGrpc.blockingStub(channel)
        val resp: CreationResponse = stub.createGrain(request)
        val f = Future.successful(resp)
        println("Returning a future " + f)
        f
      case "async" =>
        val stub = CreateGrainGrpc.stub(channel)
        val f: Future[CreationResponse] = stub.createGrain(request)
        println("Returning a future " + f)
        f
    }

  }


}
