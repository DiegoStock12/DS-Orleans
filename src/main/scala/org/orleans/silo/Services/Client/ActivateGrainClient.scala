package org.orleans.silo.Services.Client

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import io.grpc.{ManagedChannel, StatusRuntimeException}
import org.orleans.silo.activateGrain.ActivateGrainServiceGrpc.ActivateGrainServiceStub
import org.orleans.silo.activateGrain.{ActivateRequest, ActivationSuccess}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Class that you can use to execute actiavteGrain service on a remote server through gRPC call.
  */
class ActivateGrainClient(private val channel: ManagedChannel)
    extends ServiceClient(channel, new ActivateGrainServiceStub(channel))
    with LazyLogging {

  def activateGrain(name: String): Boolean = {
    logger.info("Will try to activate grain " + name + "...")
    val request = ActivateRequest(name = name)
    var reply: Boolean = false
    print(request)
    try {
      // Async test
      logger.debug("Before oncomplete")
      val f: Future[ActivationSuccess] = stub.activateGrain(request)
      f.onComplete {
        case Success(result) => reply = result.success
        case Failure(e) => {
          reply = false
          logger.warn("Grain activation failed: {}", e.getMessage)
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
