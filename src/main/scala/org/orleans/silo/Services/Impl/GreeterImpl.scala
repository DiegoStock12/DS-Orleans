package org.orleans.silo.Test

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.hello.{GreeterGrpc, HelloReply, HelloRequest}

import scala.concurrent.Future

class GreeterImpl extends GreeterGrpc.Greeter with LazyLogging {

  // TODO Can we log here not hardcoding the port.
  logger.info(s"Here greeter service waiting on port 50040 on the slave")

  override def sayHello(request: HelloRequest): Future[HelloReply] = {
    logger.debug("Received a request by " + request.name)
    val reply = HelloReply(message = "Hello " + request.name)
    Future.successful(reply)
  }
}
