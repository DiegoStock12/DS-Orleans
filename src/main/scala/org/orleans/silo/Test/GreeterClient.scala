package org.orleans.silo.Test

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import io.grpc.ManagedChannel
import org.orleans.silo.Services.Client.{SearchServiceClient, ServiceClient}
import org.orleans.silo.hello.GreeterGrpc.GreeterStub
import org.orleans.silo.hello.{HelloReply, HelloRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class GreeterClient(val channel: ManagedChannel, val stub: GreeterStub) extends ServiceClient {
  private[this] val logger = Logger.getLogger(classOf[SearchServiceClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.MILLISECONDS)
  }

  def greet(name: String): Unit = {
    logger.info("Trying to greet "+ name)
    val request = HelloRequest(name = name)
    println(request)
    try {
      val f : Future[HelloReply] = stub.sayHello(request)
      f onComplete {
        case Success(results) => println(results)
        case Failure(exception) => exception.printStackTrace()
      }

      Thread.sleep(10000)
    }
  }

}
