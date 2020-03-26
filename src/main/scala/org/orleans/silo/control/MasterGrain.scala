package org.orleans.silo.control

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.{GrainInfo, Master}
import org.orleans.silo.Services.Grain.{Grain, GrainRef}
import org.orleans.silo.Services.Grain.Grain.Receive
import org.orleans.silo.communication.ConnectionProtocol.SlaveInfo
import org.orleans.silo.dispatcher.Sender
import org.orleans.silo.storage.GrainDatabase
import org.orleans.silo.utils.GrainState

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

class MasterGrain(_id: String, master: Master)
  extends Grain(_id)
    with LazyLogging {


  logger.info("MASTER GRAIN RUNNING!")

  /**
   * Execution context of the master to run the futures
   */
  implicit val ec: ExecutionContext = master.executionContext

  override def receive: Receive = {
    case (request: SearchGrainRequest, sender: Sender) =>
      logger.info("Master grain handling grain search request")
      processGrainSearch(request, sender)

    case (request: CreateGrainRequest[_], sender: Sender) =>
      logger.info("Master grain handling create grain request")
      processCreateGrain(request, sender)

    case (request: DeleteGrainRequest, _) =>
      logger.info("Master handling delete grain request")
      processDeleteGrain(request)

  }

  /**
   * Processes a request for searching a grain and responds to the sender
   *
   * @param request
   * @param sender
   */
  private def processGrainSearch(request: SearchGrainRequest, sender: Sender): Unit = {
    val id = request.id
    if (master.grainMap.containsKey(id)) {
      val activeGrains: List[GrainInfo] = master.grainMap.get(id)
      if (activeGrains.nonEmpty) {
        val leastLoadedGrain: GrainInfo = activeGrains.reduceLeft((x, y) => if (x.load < y.load) x else y)
        sender ! SearchGrainResponse(leastLoadedGrain.address, leastLoadedGrain.port)
      }
      else {
        logger.warn("No active grain.")
        // Activate grain
        // Chose grain with least total load
        val info: SlaveInfo = master.slaves.values.reduceLeft((x, y) => if (x.totalLoad < y.totalLoad) x else y)
        val slaveRef = GrainRef(info.uuid, info.host, 1600)

      }
    }
    else {
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
  private def processCreateGrain(request: CreateGrainRequest[_], sender: Sender): Unit = {
    // Now get the only slave
    val info: SlaveInfo = master.slaves.values.reduceLeft((x, y) => if (x.totalLoad < y.totalLoad) x else y)
    val slaveRef = GrainRef(info.uuid, info.host, 1600)

    val f: Future[Any] = slaveRef ? request
    // Await result
    Await.result(f, 5 seconds)
    f onComplete {
      case Success(resp: CreateGrainResponse) =>
        // Create the grain info and put it in the grainMap
        logger.info(s"Received response from the server! $resp")
        val grainInfo = GrainInfo(info.uuid, resp.address, resp.port, GrainState.InMemory, 0)
        if (master.grainMap.containsKey(resp.id)) {
          val currentGrains: List[GrainInfo] = master.grainMap.get(resp.id)
          master.grainMap.replace(resp.id, grainInfo :: currentGrains)
        } else {
          master.grainMap.put(resp.id, List(grainInfo))
        }

        // Answer to the user
        sender ! resp

      case Failure(exception) => logger.error(s"Exeception occurred while processing create grain" +
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
        val slaveRef: GrainRef = GrainRef(grainInfo.slave, grainInfo.address, grainInfo.port)

        // Send request to the slave
        slaveRef ! request
      })

      // Delete from grainMap
      master.grainMap.remove(id)
    }
    else {
      logger.error(s"Not existing ID in the grainMap $id")
    }

  }
}
