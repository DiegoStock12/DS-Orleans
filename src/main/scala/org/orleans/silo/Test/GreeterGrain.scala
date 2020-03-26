package org.orleans.silo.Test

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.dispatcher.Sender

class GreeterGrain(_id: String) extends Grain(_id)
  with LazyLogging {

  logger.info("Greeter implementation running")

  /**
   *Receive method of the grain
   * @return
   */
  def receive = {
    case ("hi", _) =>
      logger.info("Hello back to you")
    case ("hello", sender: Sender) =>
      logger.info("Replying to the sender!")
      // Answer to the sender of the message
      // Asynchronous response
      sender ! "Hello World!"
  }
}

