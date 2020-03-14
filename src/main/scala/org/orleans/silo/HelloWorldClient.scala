package org.orleans.silo

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import org.orleans.silo.hello.{GreeterGrpc, HelloReply, HelloRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
// Async stub and Sync stub repectively
import org.orleans.silo.hello.GreeterGrpc.{GreeterStub, GreeterBlockingStub}

object HelloWorldClient{
  def apply(host: String, port : Int): HelloWorldClient = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    val stub = GreeterGrpc.stub(channel)
    new HelloWorldClient(channel, stub)
  }

  def main(args: Array[String]): Unit = {
    val client = HelloWorldClient("localhost", 50050)
    try{
      val user = args.headOption.getOrElse("World")
      client.greet(name = user)
    }
    finally {
      client.shutdown()
    }
  }
}


class HelloWorldClient private(
                                private val channel: ManagedChannel,
                                private val stub: GreeterStub
                              ) {
  private[this] val logger = Logger.getLogger(classOf[HelloWorldClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def greet(name: String): Unit = {
    logger.info("Will try to greet " + name + "...")
    val request = HelloRequest(name = name)
    print(request)
    try{
      // Async test
      println("Before oncomplete")
      val f : Future[HelloReply] = stub.sayHello(request)
      f.onComplete{
        case Success(result) => println(result.message)
        case Failure(e) => e.printStackTrace()
      }

      println("After oncomplete")
      Thread.sleep(10000)
    }
    catch{
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {}", e.getStatus)
    }
  }


}
