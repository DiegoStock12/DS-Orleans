package org.orleans.client
import main.scala.org.orleans.client.OrleansRuntime
import org.orleans.developer.twitter.{
  Twitter,
  TwitterAccount,
  TwitterAcountRef,
  TwitterRef
}
import org.orleans.silo.Services.Grain.GrainRef
import org.orleans.silo.Test.GreeterGrain

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object ClientMain {

  def main(args: Array[String]): Unit = {
    val runtime = OrleansRuntime()
      .registerGrain[GreeterGrain]
      .setHost("localhost")
      .setPort(1400)
      .build()

    var time = System.currentTimeMillis()
    val twitterFuture: Future[TwitterRef] =
      runtime.createGrain[Twitter, TwitterRef]()
    val twitter = Await.result(twitterFuture, 5 seconds)
    println(
      s"Creating a Twitter grain took ${System.currentTimeMillis() - time}ms")

    time = System.currentTimeMillis()
    val twitterWouter: TwitterAcountRef =
      Await
        .result(twitter.createAccount("wouter"), 5 seconds) match {
        case Success(ref: TwitterAcountRef) => ref
        case Failure(msg) => {
          println(msg)
          null
        }
      }

    println(s"Creating a TwitterAccount grain for Wouter took ${System
      .currentTimeMillis() - time}ms")

    time = System.currentTimeMillis()
    val twitterDiego: TwitterAcountRef =
      Await.result(twitter.createAccount("diego"), 5 seconds) match {
        case Success(ref: TwitterAcountRef) => ref
        case Failure(msg) => {
          println(msg)
          null
        }
      }
    println(s"Creating a TwitterAccount grain for Diego took ${System
      .currentTimeMillis() - time}ms")

    time = System.currentTimeMillis()
    Thread.sleep(1000)
//    val twitterWouterTwo: TwitterAcountRef =
//      Await.result(twitter.createAccount("pietje"), 5 seconds) match {
//        case Success(ref: TwitterAcountRef) => ref
//        case Failure(msg) => {
//          println(msg)
//          null
//        }
//      }
//    println(
//      s"Trying to create a second TwitterAccount grain for Wouter took ${System
//        .currentTimeMillis() - time}ms")

  }
}
