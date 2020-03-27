package org.orleans.client
import org.orleans.developer.twitter.TwitterMessages.{UserCreate, UserExists}
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
      .registerGrain[Twitter]
      .registerGrain[TwitterAccount]
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
    val twitterFuture1: Future[TwitterRef] =
      runtime.createGrain[Twitter, TwitterRef]()
    val twitter1 = Await.result(twitterFuture1, 5 seconds)
    println(
      s"Creating a Twitter grain took ${System.currentTimeMillis() - time}ms")

    time = System.currentTimeMillis()
    val twitterFuture2: Future[TwitterRef] =
      runtime.createGrain[Twitter, TwitterRef]()
    val twitter2 = Await.result(twitterFuture2, 5 seconds)
    println(twitter2.grainRef.id)
    println(
      s"Creating a Twitter grain took ${System.currentTimeMillis() - time}ms")

    //twitter.grainRef ! UserExists("wouter")
    //twitter.grainRef ! UserExists("diego")
    //twitter.grainRef ! UserCreate("wouter", "")
    //twitter.grainRef ! UserExists("wouter")

    //val listF = Future.sequence(
    //  List(1 to 100).map(x => runtime.createGrain[Twitter, TwitterRef]()))

//    time = System.currentTimeMillis()
//    val futures = Future.sequence(
//      (1 to 100).toList.map(x => twitter.createAccount(s"wouter-${x}")))
//    val results = Await.ready(futures, 10 seconds)

    time = System.currentTimeMillis()
    for (i <- (1 to 10000)) {
      Await.result(twitter.createAccount(s"wouter-${i}"), 50 seconds)
      //println(i)
    }

    //Await.result(twitter.createAccount(s"wouter-${i}"), 1 seconds)
    println(s"That took ${System.currentTimeMillis() - time}ms")

    time = System.currentTimeMillis()
    for (i <- (1 to 10000)) {
      Await.result(twitter.getAccount(s"wouter-${i}"), 50 seconds)
      //println(i)
    }

    //Await.result(twitter.createAccount(s"wouter-${i}"), 1 seconds)
    println(s"That took ${System.currentTimeMillis() - time}ms")
//    time = System.currentTimeMillis()
//    val twitterWouter: TwitterAcountRef =
//      Await
//        .result(twitter.createAccount("wouter"), 5 seconds) match {
//        case Success(ref: TwitterAcountRef) => ref
//        case Failure(msg) => {
//          println(msg)
//          null
//        }
//      }
//
//    println(s"Creating a TwitterAccount grain for Wouter took ${System
//      .currentTimeMillis() - time}ms")
//
//    time = System.currentTimeMillis()
//    val twitterDiego: TwitterAcountRef =
//      Await.result(twitter.createAccount("diego"), 5 seconds) match {
//        case Success(ref: TwitterAcountRef) => ref
//        case Failure(msg) => {
//          println(msg)
//          null
//        }
//      }
//    println(s"Creating a TwitterAccount grain for Diego took ${System
//      .currentTimeMillis() - time}ms")
//
//    time = System.currentTimeMillis()
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
