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

    val users = 10
    println(s"Now creating $users users.")
    time = System.currentTimeMillis()
    for (i <- (1 to users)) {
      val user: TwitterAcountRef =
        Await.result(twitter.createAccount(s"wouter-${i}"), 5 seconds)
    }

    //Await.result(twitter.createAccount(s"wouter-${i}"), 1 seconds)
    println(s"That took ${System.currentTimeMillis() - time}ms")

    println(s"Now searching those $users users and show following list.")
    time = System.currentTimeMillis()
    for (i <- (1 to users)) {
      val user = Await.result(twitter.getAccount(s"wouter-${i}"), 50 seconds)
      for (j <- (1 to users)) {
        if (i != j) {
          Await.result(user.followUser(twitter, s"wouter-${j}"), 5 seconds)
        }
      }

      val followers = Await.result(user.getFollowingList(), 50 seconds)
      println(s"wouter-${i} - ${followers.size} followers")
    }

    //Await.result(twitter.createAccount(s"wouter-${i}"), 1 seconds)
    println(s"That took ${System.currentTimeMillis() - time}ms")

    println(s"Now searching those $users users and send 10 000 tweets.")
    time = System.currentTimeMillis()
    for (i <- (1 to users)) {
      val user = Await.result(twitter.getAccount(s"wouter-${i}"), 50 seconds)
      var futures: List[Future[Any]] = List()
      for (j <- (1 to 5000)) {
        user.tweet("I like dis")
      }

    }
    //Await.result(twitter.createAccount(s"wouter-${i}"), 1 seconds)
    println(s"That took ${System.currentTimeMillis() - time}ms")

    Thread.sleep(5000)
    println(s"Now searching those $users users and get amount of tweets..")
    time = System.currentTimeMillis()
    for (i <- (1 to users)) {
      val user = Await.result(twitter.getAccount(s"wouter-${i}"), 50 seconds)
      val size = Await.result(user.getAmountOfTweets(), 50 seconds)
      println(s"wouter-${i} - $size tweets")
    }
    //Await.result(twitter.createAccount(s"wouter-${i}"), 1 seconds)
    println(s"That took ${System.currentTimeMillis() - time}ms")

  }
}
