package org.orleans.silo.Services.Impl

import java.lang.reflect.Method
import java.util.UUID
import java.util.concurrent.Executors.newSingleThreadExecutor

import com.typesafe.scalalogging.LazyLogging
import io.grpc.{ServerBuilder, ServerServiceDefinition}
import org.orleans.silo.Services.Client.{CreateGrainClient, ServiceFactory}
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.createGrain.CreateGrainGrpc.CreateGrain
import org.orleans.silo.createGrain.{CreationRequest, CreationResponse}
import org.orleans.silo.runtime.Runtime
import org.orleans.silo.runtime.Runtime.GrainInfo
import org.orleans.silo.utils.GrainState

import scala.concurrent.{ExecutionContext, Future}


/**
 * Some definitions that we need in order to be able to
 * infer the classes of the services
 */
object CreateGrainImpl {
  val GRPC_SUFFIX = "Grpc"
  val GRPC_SUBCLASS_SUFFIX = "Grpc$"
  val SERVICE_BINDER = "bindService"
}

/**
 * This class behaves differently depending on whether the executing server
 * is a master or a slave
 *
 * @param serverType master or slave
 */
class CreateGrainImpl(private val serverType: String,
                      private val runtime: Runtime)
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
      case "slave" => createNewGrain(request)
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
    // TODO we should look for the least loaded slave to send the info to
    val c: CreateGrainClient =
      ServiceFactory.createGrainService("localhost", 50060, stubType = "sync")
    // Just relay the request
    val f: Future[CreationResponse] = c.createGrain(request)
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
    // Info necessary for reflection of the service
    val packageName = request.packageName
    val serviceName = request.serviceName

    val serviceDefinitionClass: Class[_] = Class
      .forName(packageName + "." + serviceName + GRPC_SUFFIX)

    // Get the actual interface for that object
    val serviceInterface: Class[_] = Class
      .forName(
        packageName + "." + serviceName + GRPC_SUBCLASS_SUFFIX + serviceName)

    // Get the bindService method
    val binder: Method = serviceDefinitionClass
      .getDeclaredMethod(SERVICE_BINDER,
        serviceInterface,
        classOf[ExecutionContext])
    binder.setAccessible(true)

    // Get the service implementation that should run on that port
    // This is the object of which we keep a reference in the map
    // so we can check its status
    // First get the ID for that new grain
    val id = UUID.randomUUID().toString
    val impl = Class
      .forName(
        request.implementationPackage + "." + request.implementationName)
      .getConstructor(classOf[String])
      .newInstance(id)

    // Build the service definition by binding the servide and an execution context
    val ssd: ServerServiceDefinition = binder
      .invoke(null,
        impl.asInstanceOf[Object],
        ExecutionContext
          .fromExecutorService(newSingleThreadExecutor)
          .asInstanceOf[Object])
      .asInstanceOf[ServerServiceDefinition]


    // Get a port and an id for the new service and create it

    val port = runtime.getFreePort

    // Start the grain
    ServerBuilder
      .forPort(port)
      .addService(ssd)
      .build
      .start

    // Save the grain information in the runtime
    runtime.grainMap.put(port, GrainInfo(id, GrainState.InMemory, impl.asInstanceOf[Grain]))
    runtime.grainMap.forEach((k, v) => logger.info(s"$k --> $v"))

    Thread.sleep(500)

    // Return a response
    val response = CreationResponse(id, "localhost", port)
    Future.successful(response)
  }
}
