package org.orleans.developer.twitter

import com.typesafe.scalalogging.LazyLogging
import org.orleans.developer.twitter.TwitterMessages
import org.orleans.developer.twitter.TwitterMessages.{
  FollowUser,
  GetFollowers,
  Tweet
}
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.Services.Grain.Grain.Receive
import org.orleans.silo.dispatcher.Sender

class TwitterAccount(id: String) extends Grain(id) with LazyLogging {

  private var username: String = ""
  private var tweets: List[Tweet] = List()
  private var followers: List[String] = List()

  override def receive: Receive = {
    case (tweet: Tweet, _)                 => tweets = tweet :: tweets
    case (follow: FollowUser, _)           => followers = follow.name :: followers
    case (f: GetFollowers, sender: Sender) => sender ! followers
  }
}
