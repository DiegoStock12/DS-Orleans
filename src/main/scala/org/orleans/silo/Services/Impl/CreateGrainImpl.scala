package org.orleans.silo.Services.Impl

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.LazyLogging
import io.grpc.{ServerBuilder, ServerServiceDefinition}
import org.orleans.silo.Services.Client.{CreateGrainClient, ServiceFactory}
import org.orleans.silo.Services.Service
import org.orleans.silo.createGrain.CreateGrainGrpc.CreateGrain
import org.orleans.silo.createGrain.{CreationRequest, CreationResponse}
import org.orleans.silo.utils.GrainDescriptor
import java.util.concurrent.Executors.newSingleThreadExecutor
W
import scala.concurrent.{ExecutionContext, Future}

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

  logger.info("Started createGrain service in " + serverType)

  /**
   * Depending on the server type, either relay to a slave or create
   * the new grain while checking the runtime
   *
   * @param request
   * @return
   */
  override def createGrain(request: CreationRequest): Future[CreationResponse] = {
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
  private def relayPetition(request: CreationRequest): Future[CreationResponse] = {
    logger.info("Relaying to slave")
    val c: CreateGrainClient = ServiceFactory
      .getService(Service.CreateGrain, "localhost", 50060, stubType = "sync")
      .asInstanceOf[CreateGrainClient]
    val f: Future[CreationResponse] = c.createGrain(serviceId = request.service,
      serviceName = request.serviceDefinition)
    f
  }

  /**
   * Actually create a new grain looking at the free ports in the system
   *
   * @param request
   * @return
   */
  private def createNewGrain(request: CreationRequest): Future[CreationResponse] = {
    // get the service name (i.e CreateGrain -> maybe its CreateGrainGrpc.CreateGrain)
    logger.info("Creating the grain in the slave")
    println(request)
    //    println(Greeter.getClass.getName)
    //    println(Greeter.getClass.getMethods.foreach(println))
    // Get the object name for the service
    //    val serviceDefinitionClass: Class[_] = Class
    //      .forName("org.orleans.silo.hello."+request.serviceDefinition+"Grpc$"+request.serviceDefinition+"$")
    val serviceDefinitionClass: Class[_] = Class
      .forName("org.orleans.silo.hello." + request.serviceDefinition + "Grpc")

    // Get the actual interface for that object
    val serviceInterfaceName: Class[_] = Class
      .forName("org.orleans.silo.hello." + request.serviceDefinition + "Grpc$" + request.serviceDefinition)


    //println("Methods = " + serviceDefinitionClass.getDeclaredMethods.foreach(println))
    // Get the bindService method
    val binder: Method = serviceDefinitionClass
      .getDeclaredMethod("bindService", serviceInterfaceName, classOf[ExecutionContext])
    binder.setAccessible(true)


    val impl = Class.forName("org.orleans.silo.Services.Impl." + request.serviceDefinition + "Impl")
      .getDeclaredConstructor().newInstance()

    val ssd: ServerServiceDefinition = binder.invoke(null, impl.asInstanceOf[Object],
      ExecutionContext.fromExecutorService(newSingleThreadExecutor).asInstanceOf[Object])
      .asInstanceOf[ServerServiceDefinition]

    // TODO look at the port map of the Slave Runtime
    val port = getFreePort()
    ServerBuilder.forPort(port)
      .addService(ssd)
      .build
      .start
    val response = CreationResponse("a", "10.100.0.1", 5000)
    Future.successful(response)
  }

  /**
   * Return a free port to instantiate a new grain
   */
  private def getFreePort() = {
    // Now return a random number
    9000
  }
}
