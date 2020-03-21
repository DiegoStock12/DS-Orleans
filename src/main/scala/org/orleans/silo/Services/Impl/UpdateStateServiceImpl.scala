package org.orleans.silo.Services.Impl

import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.updateGrainState.{UpdateGrainStateServiceGrpc, UpdateStateRequest, UpdateSuccess}
import org.orleans.silo.utils.{GrainDescriptor, GrainState, SlaveDetails}
import org.orleans.silo.utils.GrainState.GrainState

import scala.concurrent.Future

/**
 * Implementation of the updateGrainState service (This one may run on master and can be called by slaves to update the global state of the grain).
 * The service is binded on the gRPC server and updateState can be called through remote call.
 */
class UpdateStateServiceImpl(var grainMap: ConcurrentHashMap[String, List[GrainDescriptor]])
  extends UpdateGrainStateServiceGrpc.UpdateGrainStateService
    with LazyLogging {

  override def updateState(request: UpdateStateRequest): Future[UpdateSuccess] = {
    logger.debug("Updating the state of the grain: {}", request.grainId)

    val grainId: String = request.grainId
    val sourceIP: String = request.sourceIP
    val state: GrainState = GrainState.withNameWithDefault(request.state)

    if (grainMap.containsKey(grainId)) {
      val grainLocations: List[GrainDescriptor] = grainMap.get(grainId)

      // Replace the description of the updated grain
      val newGrainLocations: List[GrainDescriptor] = grainLocations.map(descriptor => {
        if (descriptor.location.address.equals(sourceIP)) {
          GrainDescriptor(state, descriptor.location)
        }
        else
          descriptor
      })

      logger.debug("Updating state of the grain.")

      // Update the map with new description of the grain
      grainMap.replace(grainId, newGrainLocations)

//      println(grainLocations)
//      println(newGrainLocations)
//      println(grainMap)

    } else {
      logger.warn("Master notified about the grain it didn't know about!")
      grainMap.put(grainId, List(GrainDescriptor(state, SlaveDetails(sourceIP, 1000))))
    }
    Future.successful(UpdateSuccess(success = true))
  }
}
