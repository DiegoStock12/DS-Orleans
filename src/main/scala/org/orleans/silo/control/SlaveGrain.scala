package org.orleans.silo.control

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.Slave
import org.orleans.silo.dispatcher.{Dispatcher, Sender}

import scala.reflect.ClassTag
import scala.reflect._

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
      if (slave.grainMap.containsKey(request.id)) {
        processGrainDeletion(request)(slave.grainMap.get(request.id))
      } else {
        logger.error("ID doesn't exist in the database")
        slave.grainMap.forEach((k, v) => logger.info(s"$k, $v"))
      }

    case other =>
      logger.error(s"Unexpected message in the slave grain $other")

  }

  /**
    * Manage the creation of new grains
    *
    * @param request
    */
  def processGrainCreation[T <: Grain: ClassTag](request: CreateGrainRequest,
                                                 sender: Sender): Unit = {
    logger.info(
      s"Received creation request for grain ${request.grainClass.runtimeClass.getName}")
    // If there exists a dispatcher for that grain just add it
    if (slave.registeredGrains.contains(request.grainClass.runtimeClass)) {
      logger.info(s"Found existing dispatcher for class")

      // Add the grain to the dispatcher
      val dispatcher: Dispatcher[T] = slave.dispatchers
        .filter {
          _.isInstanceOf[Dispatcher[T]]
        }
        .head
        .asInstanceOf[Dispatcher[T]]

      // Get the ID for the newly created grain
      val id = dispatcher.addGrain()

      // Add it to the grainMap
      logger.info(s"Adding to the slave grainmap id $id")
      slave.grainMap.put(id, classTag[T])

      sender ! CreateGrainResponse(id, slave.slaveConfig.host, dispatcher.port)

      // If there's not a dispatcher for that grain type
      // create the dispatcher and add the grain
    } else {
      logger.info(
        s"Creating new dispatcher for class ${request.grainClass.runtimeClass}")
      // Create a new dispatcher for that and return its properties
      val dispatcher: Dispatcher[T] = new Dispatcher[T](slave.getFreePort)
      // Add the dispatchers to the dispatcher
      slave.dispatchers = dispatcher :: slave.dispatchers
      val id: String = dispatcher.addGrain()

      // Create and run the dispatcher Thread
      val newDispatcherThread: Thread = new Thread(dispatcher)
      newDispatcherThread.setName(
        s"Dispatcher-${slave.shortId}-${classTag[T].runtimeClass.getName}")
      newDispatcherThread.start()

      // Add it to the grainMap
      logger.info(s"Adding to the slave grainmap id $id")
      slave.grainMap.put(id, classTag[T])

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
  def processGrainDeletion[T <: Grain: ClassTag](
      request: DeleteGrainRequest): Unit = {
    // Grain id to delete
    val id = request.id
    logger.info(s"Trying to delete grain with id $id")
    // Get the appropriate dispatcher
    val dispatcher: Dispatcher[T] = slave.dispatchers
      .filter {
        _.isInstanceOf[Dispatcher[T]]
      }
      .head
      .asInstanceOf[Dispatcher[T]]

    println(s"$dispatcher - ${dispatcher.port}")

    // Delete the grain
    dispatcher.deleteGrain(id)
  }

}
