package org.orleans.silo.Services.Impl

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.updateGrainState.{
  UpdateGrainStateServiceGrpc,
  UpdateStateRequest,
  UpdateSuccess
}

import scala.concurrent.Future


/**
 * Implementation of the updateGrainState service (This one may run on master and can be called by slaves to update the global state of the grain).
 * The service is binded on the gRPC server and updateState can be called through remote call.
 */
class UpdateStateServiceImpl
    extends UpdateGrainStateServiceGrpc.UpdateGrainStateService
    with LazyLogging {

  override def updateState(
      request: UpdateStateRequest): Future[UpdateSuccess] = {
    logger.debug("Updating state of the grain.")
    val reply = UpdateSuccess(success = true)

    Future.successful(reply)
  }
}
