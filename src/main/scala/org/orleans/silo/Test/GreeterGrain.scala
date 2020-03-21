package org.orleans.silo.Test

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.hello.{GreeterGrpc, HelloReply, HelloRequest}

import scala.concurrent.Future

class GreeterGrain(_id: String) extends Grain(_id)
  with GreeterGrpc.Greeter
  with LazyLogging {

  logger.info("Greeter implementation running")
  override def sayHello(request: HelloRequest): Future[HelloReply] = {
    logger.debug("Received a request by " + request.name)
    val reply = HelloReply(message = "Hello " + request.name)
    Future.successful(reply)
  }

  override def store(): Unit = {}
}
