package org.orleans.silo.Services.Client

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import scala.concurrent.ExecutionContext.Implicits.global

import io.grpc.{ManagedChannel, StatusRuntimeException}
import org.orleans.silo.activateGrain.ActivateGrainServiceGrpc.ActivateGrainServiceStub
import org.orleans.silo.activateGrain.{ActivateRequest, ActivationSuccess}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ActivateGrainClient (private val channel: ManagedChannel,
                                private val stub: ActivateGrainServiceStub) extends ServiceClient {
  private[this] val logger = Logger.getLogger(classOf[ActivateGrainClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def activateGrain(name: String): Unit = {
    logger.info("Will try to greet " + name + "...")
    val request = ActivateRequest(name = name)
    print(request)
    try{
      // Async test
      println("Before oncomplete")
      val f : Future[ActivationSuccess] = stub.activateGrain(request)
      f.onComplete{
        case Success(result) => println(result.success)
        case Failure(e) => e.printStackTrace()
      }

      println("After oncomplete")
      Thread.sleep(10000)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {}", e.getStatus)
    }
  }
}