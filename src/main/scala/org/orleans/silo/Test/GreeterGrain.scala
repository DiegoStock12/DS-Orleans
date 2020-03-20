package org.orleans.silo.Test

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.hello.{GreeterGrpc, HelloReply, HelloRequest}

import scala.concurrent.Future

class GreeterGrain extends Grain
  with GreeterGrpc.Greeter
  with LazyLogging {

  logger.info("Here Greeter Impl!")
  logger.info("Setting the id")
  this.id = "changed id"

  override def sayHello(request: HelloRequest): Future[HelloReply] = {
    logger.debug("Received a request by " + request.name)
    val reply = HelloReply(message = "Hello " + request.name)
    Future.successful(reply)
  }

  override def store(): Unit = {}
}
