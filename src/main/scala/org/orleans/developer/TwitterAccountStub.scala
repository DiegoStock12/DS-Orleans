package org.orleans.developer
import org.orleans.silo.communication.GrainChannel
import org.orleans.silo.twitterAcount.{TweetAck, TweetRequest}
import org.orleans.silo.twitterAcount.TwitterGrpc.TwitterStub

import scala.concurrent.Future

class TwitterAccountStub(id: String, channel: GrainChannel[TwitterAcount]) {

  def tweet(tweet: Tweet): Future[TweetAck] = {
    channel.send(id, TweetRequest(tweet.timestamp, tweet.msg))
  }
}
