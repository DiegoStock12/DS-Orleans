package org.orleans.silo.control

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.Slave
import org.orleans.silo.dispatcher.{Dispatcher, Sender}

import scala.reflect.ClassTag


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
  override def receive = {

    // Process creation requests
    case (request: CreateGrainRequest, sender: Sender) =>
      logger.info("Slave grain processing grain creation request")
      processGrainCreation(request, sender)(request.grainClass)

    // Process deletion requests
    case (request: DeleteGrainRequest, _) =>
      slave.grainMap.get(request.id) match {
        case Some(value)=> processGrainDeletion(request)(value)
        case None => logger.error("ID doesn't exist in the database")
      }

    case other =>
      logger.error(s"Unexpected message in the slave grain $other")

  }

  /**
   * Manage the creation of new grains
   *
   * @param request
   */
  def processGrainCreation[T <: Grain : ClassTag](request: CreateGrainRequest, sender: Sender): Unit = {
    logger.info(s"Received creation request for grain ${request.grainClass.runtimeClass.getName}")
    // If there exists a dispatcher for that grain just add it
    if (slave.registeredGrains.contains(request.grainClass)) {
      logger.info(s"Found existing dispatcher for class")

      // Add the grain to the dispatcher
      val dispatcher: Dispatcher[T] = slave.dispatchers.filter {
        _.isInstanceOf[Dispatcher[T]]
      }.head.asInstanceOf[Dispatcher[T]]

      // Get the ID for the newly created grain
      val id = dispatcher.addGrain()
      sender ! CreateGrainResponse(id, slave.slaveConfig.host, dispatcher.port)

      // If there's not a dispatcher for that grain type
      // create the dispatcher and add the grain
    } else {
      logger.info("Creating new dispatcher for class")
      // Create a new dispatcher for that and return its properties
      val dispatcher: Dispatcher[_ <: Grain] = new Dispatcher[T](slave.getFreePort)
      val id: String = dispatcher.addGrain()
      // Return the newly created information
      sender ! CreateGrainResponse(id, slave.slaveConfig.host, dispatcher.port)
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
    // Get the appropriate dispatcher
    val dispatcher: Dispatcher[T] = slave.dispatchers.filter {
      _.isInstanceOf[Dispatcher[T]]
    }.head.asInstanceOf[Dispatcher[T]]

    // Delete the grain
    dispatcher.deleteGrain(id)
  }


}
