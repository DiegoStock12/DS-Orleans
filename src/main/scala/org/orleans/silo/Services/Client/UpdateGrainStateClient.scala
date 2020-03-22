package org.orleans.silo.Services.Client

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import io.grpc.{ManagedChannel, StatusRuntimeException}
import org.orleans.silo.updateGrainState.UpdateGrainStateServiceGrpc.UpdateGrainStateServiceStub
import org.orleans.silo.updateGrainState.{UpdateStateRequest, UpdateSuccess}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Class that you can use to execute updateGrainState service on a remote server through gRPC call. Slave node can use
  * it to notify the master abut changes in a state of the grains it manages.
  */
class UpdateGrainStateClient(private val channel: ManagedChannel)
    extends ServiceClient(channel, new UpdateGrainStateServiceStub(channel))
    with LazyLogging {

  def updateGrainState(grainId: String,
                       newState: String,
                       source: String): Boolean = {
    logger.info(
      "Will try to update state of the grain " + grainId + " to " + newState.toString)
    val request = UpdateStateRequest(grainId, newState, source)
    var reply: Boolean = false
    print(request)
    try {
      // Async test
      logger.debug("Before oncomplete")
      val f: Future[UpdateSuccess] = stub.updateState(request)
      f.onComplete {
        case Success(result) => reply = result.success
        case Failure(e) => {
          reply = false
          logger.warn("Grain status updated: {}", e.getMessage)
        }
      }
      logger.debug("After oncomplete")
      reply
    } catch {
      case e: StatusRuntimeException =>
        logger.warn("RPC failed: {}", e.getStatus)
        false
    }
  }
}
