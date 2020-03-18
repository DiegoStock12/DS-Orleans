package org.orleans.silo.Services.Impl

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.hello.{GreeterGrpc, HelloReply, HelloRequest}

import scala.concurrent.Future

class GreeterImpl extends GreeterGrpc.Greeter with LazyLogging {

  logger.info("Here Greeter Impl!")
  override def sayHello(request: HelloRequest): Future[HelloReply] = {
    logger.debug("Received a request by " + request.name)
    val reply = HelloReply(message = "Hello " + request.name)
    Future.successful(reply)
  }
}
