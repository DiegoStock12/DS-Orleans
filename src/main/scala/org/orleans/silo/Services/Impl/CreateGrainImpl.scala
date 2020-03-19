package org.orleans.silo.Services.Impl

import java.lang.reflect.Method
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.LazyLogging
import io.grpc.{ServerBuilder, ServerServiceDefinition}
import org.orleans.silo.Services.Client.{CreateGrainClient, ServiceFactory}
import org.orleans.silo.createGrain.CreateGrainGrpc.CreateGrain
import org.orleans.silo.createGrain.{CreationRequest, CreationResponse}
import org.orleans.silo.utils.GrainDescriptor
import java.util.concurrent.Executors.newSingleThreadExecutor

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random


/**
 * Some definitions that we need in order to be able to
 * infer the classes of the services
 */
object CreateGrainImpl{
  val SERVICE_DEFINITION_PACKAGE = "org.orleans.silo."
  val SERVICE_IMPLEMENTATION_PACKAGE = "org.orleans.silo.Services.Impl."
  val GRPC_SUFFIX = "Grpc"
  val GRPC_SUBCLASS_SUFFIX = "Grpc$"
  val IMPLEMENTATION_SUFFIX = "Impl"
  val SERVICE_BINDER = "bindService"
}

/**
  * This class behaves differently depending on whether the executing server
  * is a master or a slave
  *
  * @param serverType master or slave
  */
class CreateGrainImpl(val serverType: String,
                      val grainMap: ConcurrentHashMap[String, GrainDescriptor])
    extends CreateGrain
    with LazyLogging {

  import CreateGrainImpl._
  logger.info("Started createGrain service in " + serverType)

  /**
    * Depending on the server type, either relay to a slave or create
    * the new grain while checking the runtime
    *
    * @param request
    * @return
    */
  override def createGrain(
      request: CreationRequest): Future[CreationResponse] = {
    serverType match {
      case "master" => relayPetition(request)
      case "slave"  => createNewGrain(request)
    }
  }

  /**
    * In case it's the master service executing this, we simply relay the
    * petition to the server
    *
    * @return
    */
  private def relayPetition(
      request: CreationRequest): Future[CreationResponse] = {
    logger.info("Relaying to slave")
    val c: CreateGrainClient =
      ServiceFactory.createGrainService("localhost", 50060, stubType = "sync")
    // Just relay the request
    val f: Future[CreationResponse] = c.createGrain(request.serviceDefinition)
    f
  }

  /**
    * Actually create a new grain looking at the free ports in the system
    *
    * @param request
    * @return
    */
  private def createNewGrain(
      request: CreationRequest): Future[CreationResponse] = {
    logger.info("Creating the grain in the slave")

    // TODO find an elegant solution to the package name insise the ServiceFolder
    val serviceDefinitionClass: Class[_] = Class
      .forName(SERVICE_DEFINITION_PACKAGE +"hello."+ request.serviceDefinition + GRPC_SUFFIX)

    // Get the actual interface for that object
    val serviceInterfaceName: Class[_] = Class
      .forName(
        SERVICE_DEFINITION_PACKAGE +"hello."+ request.serviceDefinition + GRPC_SUBCLASS_SUFFIX + request.serviceDefinition)

    // Get the bindService method
    val binder: Method = serviceDefinitionClass
      .getDeclaredMethod(SERVICE_BINDER,
                         serviceInterfaceName,
                         classOf[ExecutionContext])
    binder.setAccessible(true)

    // Get the service implementation that should run on that port
    val impl = Class
      .forName(
        SERVICE_IMPLEMENTATION_PACKAGE + request.serviceDefinition + IMPLEMENTATION_SUFFIX)
      .getDeclaredConstructor()
      .newInstance()

    // Build the service definition by binding the servide and an execution context
    val ssd: ServerServiceDefinition = binder
      .invoke(null,
              impl.asInstanceOf[Object],
              ExecutionContext
                .fromExecutorService(newSingleThreadExecutor)
                .asInstanceOf[Object])
      .asInstanceOf[ServerServiceDefinition]


    // Get a port and an id for the new service and create it
    val id = UUID.randomUUID().toString
    val port = getFreePort()
    ServerBuilder
      .forPort(port)
      .addService(ssd)
      .build
      .start

    // Return a response
    val response = CreationResponse(id, "localhost", port)
    Future.successful(response)
  }

  /**
    * Return a free port to instantiate a new grain
    */
  // TODO look at the port map of the Slave Runtime to get the port
  private def getFreePort() = {
    // Now return a random number
    val r = Random
    r.nextInt(5000)+6000
  }
}
