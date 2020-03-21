package org.orleans.silo.Services.Client

import java.util.concurrent.{AbstractExecutorService, TimeUnit}

import com.typesafe.scalalogging.LazyLogging
import io.grpc.ManagedChannel
import org.orleans.silo.createGrain.{CreateGrainGrpc, CreationRequest, CreationResponse}
import scalapb.grpc.AbstractService

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.reflect._
import org.orleans.silo.Services.Grain.Grain

class CreateGrainClient(val channel: ManagedChannel,
                        val stubType: String = "async")
  extends ServiceClient
    with LazyLogging {

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.MILLISECONDS)
  }

  /**
   * Creates a grain in a remote server of the type specified
   *
   * @tparam T Class of the rpc service
   * @tparam I Implementation of the service
   * @return
   */
  def createGrain[T <: AbstractService with AnyRef : ClassTag,
    I <: Grain with AnyRef : ClassTag](): Future[CreationResponse] = {
    logger.info("Sending service implementation " + classTag[T].runtimeClass)
    // Build a request with the desired info so we can reflect the class
    val request = CreationRequest(serviceName = classTag[T].runtimeClass.getSimpleName,
      packageName = classTag[T].runtimeClass.getPackage.getName,
      implementationName = classTag[I].runtimeClass.getSimpleName,
      implementationPackage = classTag[I].runtimeClass.getPackage.getName)
    println(request)
    sendRequest(request, stubType)
  }

  /**
   * Used by the master to relay the request to a certain slave
   *
   * @param creationRequest the request to be sent to the worker
   * @return
   */
  def createGrain(creationRequest: CreationRequest): Future[CreationResponse] = {
    sendRequest(creationRequest, stubType)
  }


  /**
   * Send request in an async or sync mode
   *
   * @param request  Creation Request to send
   * @param stubType Either sync or async for blocking or non blocking behavior
   * @return A future with the Creation Response
   */
  private def sendRequest(request: CreationRequest, stubType: String) = {
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
