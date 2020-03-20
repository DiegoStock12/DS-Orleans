package org.orleans.silo.Services.Impl

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.activateGrain.{ActivateGrainServiceGrpc, ActivateRequest, ActivationSuccess}

import scala.concurrent.Future

/**
 * Implementation of the activateGrain service. The service is binded on the gRPC server
 * and activateGrain can be called through remote call.
 */
class ActivateGrainImpl
    extends ActivateGrainServiceGrpc.ActivateGrainService
    with LazyLogging {

  //TODO Rethink activatng the grain when persistent storage is available
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
