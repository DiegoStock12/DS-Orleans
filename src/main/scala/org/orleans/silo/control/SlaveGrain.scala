package org.orleans.silo.control

import com.sun.tools.javac.code.TypeTag
import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.Services.Grain.Grain.Receive
import org.orleans.silo.Slave
import org.orleans.silo.dispatcher.{Dispatcher, Sender}
import org.orleans.silo.storage.DatabaseConnectionExample.logger
import org.orleans.silo.storage.MongoDatabase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.reflect._
import scala.util.{Failure, Success}


/**
 * Grain that will run on the server to perform control operations
 *
 * @param _id id of the grain to be created
 * @param slave reference to the slave holding this main grain
 *
 */
class SlaveGrain(_id: String, slave: Slave)
    extends Grain(_id)
    with LazyLogging {

  logger.info("SLAVE GRAIN RUNNING!")


  /**
   * Depending on the message we receive we perform
   * one operation or the other
   *
   * @return
   */
  override def receive: Receive = {

    // Process creation requests
    case (request: CreateGrainRequest[_], sender: Sender) =>
      logger.info(s"Slave grain processing grain creation request with classtag: ${request.grainClass} and typetag:  ${request.grainType}")
      logger.info("")
      processGrainCreation(request, sender)(request.grainClass, request.grainType)

    case (request: ActivateGrainRequest, sender: Sender) =>
      logger.info("Slave grain processing grain activation request")
      processGrainActivation(request, sender)(request.grainClass)

    // Process deletion requests
    case (request: DeleteGrainRequest, _) =>
      if (slave.grainMap.containsKey(request.id)){
        processGrainDeletion(request)(slave.grainMap.get(request.id))
      } else{
        logger.error("ID doesn't exist in the database")
        slave.grainMap.forEach((k ,v) => logger.info(s"$k, $v"))
      }

    case (request: ActiveGrainRequest, sender) =>
      processGrainActivation(request)

    case other =>
      logger.error(s"Unexpected message in the slave grain $other")

  }

  /**
   * Manage the creation of new grains
   *
   * @param request
   */
  def processGrainCreation[T <: Grain : ClassTag : TypeTag](request: CreateGrainRequest[T], sender: Sender) = {
    logger.info(s"Received creation request for grain ${request.grainClass.runtimeClass.getName}")

  // If there exists a dispatcher for that grain just add it
    if (slave.registeredGrains.contains(Tuple2(request.grainClass, request.grainType))) {
      logger.info(s"Found existing dispatcher for class")

      // Add the grain to the dispatcher
      val dispatcher: Dispatcher[T] = slave.dispatchers.filter {
        _.isInstanceOf[Dispatcher[T]]
      }.head.asInstanceOf[Dispatcher[T]]

      logger.info(s"grainType: ${request.grainType} typetag: $typeTag")

      // Get the ID for the newly created grain
      // It is necessary to add the typeTag here because the dispacther type is eliminated by type erasure
      val id = dispatcher.addGrain(typeTag)

      // Add it to the grainMap
      logger.info(s"Adding to the slave grainmap id $id")
      slave.grainMap.put(id, classTag[T])

      sender ! CreateGrainResponse(id, slave.slaveConfig.host, dispatcher.port)

      // If there's not a dispatcher for that grain type
      // create the dispatcher and add the grain
    } else {
      logger.info("Creating new dispatcher for class")
      // Create a new dispatcher for that and return its properties
      val dispatcher: Dispatcher[T] = new Dispatcher[T](slave.getFreePort)
      // Add the dispatchers to the dispatcher
      slave.dispatchers = dispatcher :: slave.dispatchers
      val id: String = dispatcher.addGrain(typeTag)

      // Create and run the dispatcher Thread
      val newDispatcherThread : Thread = new Thread(dispatcher)
      newDispatcherThread.setName(s"Dispatcher-${slave.shortId}-${classTag[T].runtimeClass.getName}")
      newDispatcherThread.start()

      // Add it to the grainMap
      logger.info(s"Adding to the slave grainmap id $id")
      slave.grainMap.put(id, classTag[T])

      // Return the newly created information
      sender ! CreateGrainResponse(id, slave.slaveConfig.host, dispatcher.port)
    }

  }

  /**
   * Manage the activation of existing grain
   *
   * @param request
   */
  def processGrainActivation[T <: Grain : ClassTag](request: ActivateGrainRequest, sender: Sender): Unit = {
    logger.info("Activating the grain")

    if (slave.registeredGrains.contains(request.grainClass)) {
      logger.info(s"Found existing dispatcher for activating class")

      val dispatcher: Dispatcher[T] = slave.dispatchers.filter {
        _.isInstanceOf[Dispatcher[T]]
      }.head.asInstanceOf[Dispatcher[T]]

      dispatcher.addActivation(request.id, request.grainClass)
    } else {
      val dispatcher: Dispatcher[_ <: Grain] = new Dispatcher[T](slave.getFreePort)
      dispatcher.addActivation(request.id, request.grainClass)
    }
  }

  /**
   * Processes the deletion of a grain
   *
   * @param request request containing the ID of a grain to delete
   * @tparam T class of the grain and dispatcher
   */
  def processGrainDeletion[T <: Grain : ClassTag](request: DeleteGrainRequest): Unit = {
    // Grain id to delete
    val id = request.id
    logger.info(s"Trying to delete grain with id $id")
    // Get the appropriate dispatcher
    val dispatcher: Dispatcher[T] = slave.dispatchers.filter {
      _.isInstanceOf[Dispatcher[T]]
    }.head.asInstanceOf[Dispatcher[T]]

    println(s"$dispatcher - ${dispatcher.port}")

    // Delete the grain
    dispatcher.deleteGrain(id)
  }

  def processGrainActivation[T <: Grain : ClassTag](request: ActiveGrainRequest) = ???

}
