package org.orleans.silo.Test

import ch.qos.logback.classic.Level
import main.scala.org.orleans.client.{OrleansRuntime, OrleansRuntimeBuilder}
import org.orleans.developer.{Tweet, TwitterAccountClient, TwitterAcount}
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

    var timeMs = System.currentTimeMillis()
    val f = runtime.createGrain[TwitterAcount]()

    Await.result(f, 10 seconds)
    println(
      f"For the first grain it took ${System.currentTimeMillis() - timeMs}ms")

    timeMs = System.currentTimeMillis()
    //val g = runtime.createGrain[TwitterAcount]()

    //Await.result(g, 10 seconds)
    println(
      f"For the second grain it took ${System.currentTimeMillis() - timeMs}ms")

    val id = "2ea4a6d4-a2f1-4589-87f2-3c398ab5c95b" //get ID somehow
    timeMs = System.currentTimeMillis()
    val client =
      runtime.getGrain[TwitterAcount](id).asInstanceOf[TwitterAccountClient]

    println(
      f"For the search grain it took ${System.currentTimeMillis() - timeMs}ms")

    client.tweet(Tweet("Another tweet", System.currentTimeMillis().toString))
    val tweets = client.getAmountOfTweets()
    val tweetList = client.getTweets()

    Await.result(tweets, 10 seconds)
    println(tweets.value)

    Await.result(tweetList, 5 seconds)
    tweetList onComplete {
      case Success(value) => {
        println(value.tweets.size)
        println(value.tweets(0))
        value.tweets.foreach(println(_))
      }
      case _ =>
    }

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
