package org.orleans.silo.Test

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.hello.{HelloReply, HelloRequest}

import scala.concurrent.Future

class GreeterGrain(_id: String) extends Grain(_id)
  with LazyLogging {

  // Specify the type of requests and replies
  type Request = HelloRequest
  type Reply = HelloReply

  logger.info("Greeter implementation running")

  override def store(): Unit = {}

  /**
   *
   * @return
   */
   def receive = {
    case req: HelloRequest =>
      logger.info("Received " + req)
      HelloReply("Hello " + req.name)
    case _ =>
      logger.info("Message not supported")
  }
}
