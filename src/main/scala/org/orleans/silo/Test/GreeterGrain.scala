package org.orleans.silo.Test

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.dispatcher.Sender
import org.orleans.silo.hello.{HelloReply, HelloRequest}

class GreeterGrain(_id: String) extends Grain(_id)
  with LazyLogging {

  // Specify the type of requests and replies
  type Request = HelloRequest
  type Reply = HelloReply

  logger.info("Greeter implementation running")

  /**
   *Receive method of the grain
   * @return
   */
  def receive = {
    case ("hello", _) =>
      logger.info("Hello back to you")
    case (_, sender: Sender) =>
      logger.info("replying")
      // Answer to the sender of the message
      // Asynchronous response
      sender ! "replying to you!"
  }
}

