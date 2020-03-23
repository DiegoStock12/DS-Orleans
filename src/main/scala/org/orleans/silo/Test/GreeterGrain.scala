package org.orleans.silo.Test

import java.io.ObjectOutputStream

import com.typesafe.scalalogging.LazyLogging
import javax.naming.spi.DirStateFactory.Result
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
    case "hello" =>
      logger.info("Hello back to you")
      Some("Hello back to you!")
    case num: Int =>
      Some(num+1)
    case other =>
      logger.info("Received other")
      logger.info(s"$other")
      None

  }
}
