package org.orleans.developer
import com.typesafe.scalalogging.LazyLogging
import io.grpc.ManagedChannel
import org.orleans.silo.Services.Client.ServiceClient
import org.orleans.silo.twitterAcount._
import org.orleans.silo.twitterAcount.TwitterGrpc.TwitterStub

import scala.concurrent.Future

class TwitterAccountClient(channel: ManagedChannel)
    extends ServiceClient(channel, new TwitterStub(channel))
    with LazyLogging {

  def tweet(tweet: Tweet): Future[TweetAck] =
    stub.tweet(TweetRequest(tweet.timestamp, tweet.msg))

  def getAmountOfTweets(): Future[NumberOfTweets] =
    stub.getAmountOfTweets(GetAmountOfTweets())

  def getTweets(): Future[TweetList] = stub.getTweetList(GetTweets())

}
