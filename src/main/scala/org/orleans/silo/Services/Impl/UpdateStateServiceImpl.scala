package org.orleans.silo.Services.Impl

import org.orleans.silo.updateGrainState.{UpdateGrainStateServiceGrpc, UpdateStateRequest, UpdateSuccess}

import scala.concurrent.Future


class UpdateStateServiceImpl extends UpdateGrainStateServiceGrpc.UpdateGrainStateService {
  override def updateState(request: UpdateStateRequest):  Future[UpdateSuccess] = {
    println("Updaeting state of the grain")

    val reply = UpdateSuccess(success = true)

    Future.successful(reply)
  }
}