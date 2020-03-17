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
class ActivateGrainClient(private val channel: ManagedChannel,
                          private val stub: ActivateGrainServiceStub)
    extends ServiceClient
    with LazyLogging {

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def activateGrain(name: String): Unit = {
    logger.info("Will try to greet " + name + "...")
    val request = ActivateRequest(name = name)
    print(request)
    try {
      // Async test
      logger.debug("Before oncomplete")
      val f: Future[ActivationSuccess] = stub.activateGrain(request)
      f.onComplete {
        case Success(result) => println(result.success)
        case Failure(e)      => e.printStackTrace()
      }

      logger.debug("After oncomplete")
      Thread.sleep(10000)
    } catch {
      case e: StatusRuntimeException =>
        logger.warn("RPC failed: {}", e.getStatus)
    }
  }
}
