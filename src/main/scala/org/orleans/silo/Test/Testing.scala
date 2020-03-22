package org.orleans.silo.Test

import ch.qos.logback.classic.Level
import main.scala.org.orleans.client.{OrleansRuntime, OrleansRuntimeBuilder}
import org.orleans.developer.{TwitterAccountClient, TwitterAcount}
import org.orleans.silo.Services.Client.ServiceFactory
import org.orleans.silo.twitterAcount.TwitterGrpc
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import scala.concurrent.Await
import scala.util.{Failure, Success}

object Testing {
  // Just a test for the new Service client
  def main(args: Array[String]): Unit = {
    setLevel(Level.INFO)

    val runtime = OrleansRuntimeBuilder()
      .host("localhost")
      .port(50050)
      .registerGrain[TwitterAcount, TwitterAccountClient, TwitterGrpc.Twitter]
      .build()

    val f = runtime.createGrain[TwitterAcount]()

    Await.result(f, 10 seconds)
    f onComplete {
      case Success(res) => println(s"Success response $res")
      case Failure(e)   => e.printStackTrace()
    }

    val id = "" //get ID somehow
    val client = runtime.getGrain[TwitterAcount](id)

  }

  /** Very hacky way to set the log level */
  def setLevel(level: Level) = {
    val logger: ch.qos.logback.classic.Logger =
      org.slf4j.LoggerFactory
        .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
        .asInstanceOf[(ch.qos.logback.classic.Logger)]
    logger.setLevel(level)
  }
}
