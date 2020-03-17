package org.orleans.silo.Services.Impl

import com.typesafe.scalalogging.LazyLogging
import org.orleans.common.Grain
import org.orleans.silo.activateGrain.{
  ActivateGrainServiceGrpc,
  ActivateRequest,
  ActivationSuccess
}

import scala.concurrent.Future

class ActivateGrainImpl
    extends ActivateGrainServiceGrpc.ActivateGrainService
    with LazyLogging {

  override def activateGrain(
      request: ActivateRequest): Future[ActivationSuccess] = {
    logger.debug("Activating grain " + request.name)

    val newActivation =
      Class.forName(request.name).getDeclaredConstructor().newInstance()
    val activation = newActivation.asInstanceOf[Grain]

    activation.store()

    val reply = ActivationSuccess(success = true)

    Future.successful(reply)
  }
}
