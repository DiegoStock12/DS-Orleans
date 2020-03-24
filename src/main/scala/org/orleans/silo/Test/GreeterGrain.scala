package org.orleans.silo.Test

import java.io.ObjectOutputStream

import com.typesafe.scalalogging.LazyLogging
import javax.naming.spi.DirStateFactory.Result
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.dispatcher.Sender
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
   *Receive method of the grain
   * @return
   */
  def receive = {
    case ("hello", sender) =>
      logger.info("Hello back to you")
    case (_, sender: Sender) =>
      logger.info("replying")
      sender ! "replying to you!"
  }
}
