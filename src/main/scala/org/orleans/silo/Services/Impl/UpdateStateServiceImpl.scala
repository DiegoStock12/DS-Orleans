package org.orleans.silo.Services.Impl

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.updateGrainState.{
  UpdateGrainStateServiceGrpc,
  UpdateStateRequest,
  UpdateSuccess
}

import scala.concurrent.Future

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
