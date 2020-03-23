package org.orleans.silo.Test

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.hello.{GreeterGrpc, HelloReply, HelloRequest}

import scala.concurrent.Future

class GreeterGrain(_id: String) extends Grain(_id)
  with LazyLogging {

  // Specify the type of requests and replies
  type Request = HelloRequest
  type Reply = HelloReply

  logger.info("Greeter implementation running")

  override def store(): Unit = {}

  /**
   * Override the receive method
   * @param request message received
   * @return
   */
  override def receive(request: HelloRequest): HelloReply = {
    logger.info("Received "+HelloRequest)
    HelloReply("Hello "+ request.name)
  }
}
