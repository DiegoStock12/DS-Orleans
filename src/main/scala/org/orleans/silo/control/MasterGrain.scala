package org.orleans.silo.control

import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain.Receive
import org.orleans.silo.{GrainInfo, Master}
import org.orleans.silo.Services.Grain.{Grain, GrainRef}
import org.orleans.silo.communication.ConnectionProtocol.SlaveInfo
import org.orleans.silo.dispatcher.Sender
import org.orleans.silo.utils.{Circular, GrainState}
import org.orleans.silo.storage.GrainDatabase
import org.orleans.silo.utils.GrainState
import org.orleans.silo.utils.GrainState.GrainState

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

class MasterGrain(_id: String, master: Master)
    extends Grain(_id)
    with LazyLogging {

  private var slaveRefs: Circular[(SlaveInfo, GrainRef)] = null
  private var slaveGrainRefs: ConcurrentHashMap[String, GrainRef] =
    new ConcurrentHashMap[String, GrainRef]()

  logger.info("MASTER GRAIN RUNNING!")

  def roundRobin(): (SlaveInfo, GrainRef) = {
    if (slaveRefs == null) {
      slaveRefs = new Circular(
        master.getSlaves().map(x => (x, GrainRef(x.uuid, x.host, x.tcpPort))))
    }

    val head = slaveRefs.next

    (head._1, head._2)
  }

  /**
    * Execution context of the master to run the futures
    */
  implicit val ec: ExecutionContext = master.executionContext

  override def receive: Receive = {
    case (request: SearchGrainRequest, sender: Sender) =>
      logger.debug("Master grain handling grain search request")
      processGrainSearch(request, sender)

    case (request: CreateGrainRequest[_], sender: Sender) =>
      logger.debug("Master grain handling create grain request")
      processCreateGrain(request, sender)

    case (request: DeleteGrainRequest, _) =>
      logger.debug("Master handling delete grain request")
      processDeleteGrain(request)

    case (request: UpdateGrainStateRequest, sender: Sender) =>
      logger.info("Master handling update grain state request")
      processUpdateState(request)

  }

  /**
    * Processes a request for searching a grain and responds to the sender
    *
    * @param request
    * @param sender
    */
  private def processGrainSearch(request: SearchGrainRequest,
                                 sender: Sender): Unit = {
    val id = request.id
    if (master.grainMap.containsKey(id)) {
      val activeGrains: List[GrainInfo] = master.grainMap
        .get(id)
        .filter(grain => GrainState.InMemory.equals(grain.state))
      if (activeGrains.nonEmpty) {
        val leastLoadedGrain: GrainInfo =
          activeGrains.reduceLeft((x, y) => if (x.load < y.load) x else y)
        sender ! SearchGrainResponse(leastLoadedGrain.address,
                                     leastLoadedGrain.port)
      } else {
        logger.warn("No active grain.")
        // Activate grain
        // Chose grain with least total load
        val info: SlaveInfo = master.slaves.values.reduceLeft((x, y) =>
          if (x.totalLoad < y.totalLoad) x else y)

        var slaveRef: GrainRef = null
        if (!slaveGrainRefs.containsKey(info)) {
          slaveRef = GrainRef(info.uuid, info.host, info.tcpPort)
          slaveGrainRefs.put(info.uuid, slaveRef)
        } else {
          slaveRef = slaveGrainRefs.get(info)
        }

      }
    } else {
      //TODO See how we manage exceptions in this side!
      sender ! SearchGrainResponse(null, 0)
    }
  }

  /**
    * Choose one slave to run the new grain
    *
    * @param request
    * @param sender
    */
  private def processCreateGrain(request: CreateGrainRequest[_],
                                 sender: Sender): Unit = {

    // Now get the least loaded slave
    val info: SlaveInfo = master.slaves.values.reduceLeft((x, y) =>
      if (x.totalLoad < y.totalLoad) x else y)

    var slaveRef: GrainRef = null
    if (!slaveGrainRefs.containsKey(info.uuid)) {
      slaveRef = GrainRef(info.uuid, info.host, info.tcpPort)
      slaveGrainRefs.put(info.uuid, slaveRef)
    } else {
      slaveRef = slaveGrainRefs.get(info.uuid)
    }

    val f: Future[Any] = slaveRef ? request
    f onComplete {
      case Success(resp: CreateGrainResponse) =>
        // Create the grain info and put it in the grainMap
        logger.debug(s"Received response from a client! $resp")
        val grainInfo =
          GrainInfo(info.uuid, resp.address, resp.port, GrainState.InMemory, 0)
        if (master.grainMap.containsKey(resp.id)) {
          val currentGrains: List[GrainInfo] = master.grainMap.get(resp.id)
          master.grainMap.replace(resp.id, grainInfo :: currentGrains)
        } else {
          master.grainMap.put(resp.id, List(grainInfo))
        }

        // Answer to the user
        sender ! resp

      case Failure(exception) =>
        logger.error(
          s"Exeception occurred while processing create grain" +
            s" ${exception.printStackTrace()}")
    }
  }

  /**
    * Send the delete grain request to the appropriate Slave
    *
    * @param request
    */
  private def processDeleteGrain(request: DeleteGrainRequest): Unit = {
    val id = request.id
    // Look for the slave that has that request
    if (master.grainMap.containsKey(id)) {
      val grainInfos: List[GrainInfo] = master.grainMap.get(id)
      grainInfos.foreach(grainInfo => {
        // Send deletion request
        var slaveRef: GrainRef = null

        if (!slaveGrainRefs.containsKey(grainInfo.slave)) {
          slaveRef = GrainRef(grainInfo.slave,
                              grainInfo.address,
                              master.slaves.get(grainInfo.slave).get.tcpPort)
          slaveGrainRefs.put(grainInfo.slave, slaveRef)
        } else {
          slaveRef = slaveGrainRefs.get(grainInfo.slave)
        }

        // Send request to the slave
        slaveRef ! request
      })

      // Delete from grainMap
      master.grainMap.remove(id)
    } else {
      logger.error(s"Not existing ID in the grainMap $id")
    }

  }

  //TODO Decide when the salve deactivates the grain and send update state message
  private def processUpdateState(request: UpdateGrainStateRequest): Unit = {
    logger.debug(s"Updating state of the grain ${request.id}.")
    val newState: GrainState = GrainState.withNameWithDefault(request.state)
    val slave: String = request.source
    val port: Int = request.port
    if (master.grainMap.containsKey(request.id)) {
      val grainLocations: List[GrainInfo] = master.grainMap.get(request.id)
      // Replace the description of the updated grain
      grainLocations.foreach(grain => {
        if (grain.address.equals(slave) && grain.port.equals(port)) {
          grain.state = newState
        }
      })
    } else {
      logger.warn("Master notified about the grain it didn't know about!")
      master.grainMap
        .put(request.id, List(GrainInfo(slave, slave, port, newState, 0)))
    }

  }
}
