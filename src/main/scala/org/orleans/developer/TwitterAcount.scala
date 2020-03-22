package org.orleans.developer
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.twitterAcount._
import org.orleans.silo.twitterAcount.TwitterGrpc.Twitter

import scala.collection.mutable
import scala.concurrent.Future

case class Tweet(msg: String, timestamp: String)

class TwitterAcount(name: String) extends Grain(name) with Twitter {

  var tweets: List[Tweet] = List()

  override def tweet(
      request: TweetRequest
  ): Future[
    TweetAck
  ] = {
    tweets = Tweet(request.tweet, request.timestamp) :: tweets
    Future.successful(TweetAck(true))
  }

  override def getAmountOfTweets(
      request: GetAmountOfTweets
  ): Future[
    NumberOfTweets
  ] = {
    Future.successful(NumberOfTweets(tweets.size))
  }

  override def getTweetList(
      request: GetAmountOfTweets
  ): Future[
    TweetList
  ] = {
    Future.successful(TweetList(tweets.map(_.msg)))
  }

  override def store(): Unit = {}
}
