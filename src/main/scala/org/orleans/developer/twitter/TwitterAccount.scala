package org.orleans.developer.twitter

import java.util
import java.util.Collections

import com.typesafe.scalalogging.LazyLogging
import org.orleans.developer.twitter.TwitterMessages
import org.orleans.developer.twitter.TwitterMessages._
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.Services.Grain.Grain.Receive
import org.orleans.silo.dispatcher.Sender
import collection.JavaConverters._

class TwitterAccount(id: String) extends Grain(id) with LazyLogging {

  private var username: String = ""

  //private var tweets: util.List[Tweet] =
  //  Collections.synchronizedList(new util.ArrayList[Tweet]())
  //private var followers: util.List[String] =
  //  Collections.synchronizedList(new util.ArrayList[String]())

  private var tweets: List[Tweet] = List()
  private var followers: List[String] = List()

  override def receive: Receive = {
    case (uname: SetUsername, _) => this.username = uname.name
    case (tweet: Tweet, _) => {
      if (tweets.size >= 4500) {
        logger.info(s"Jeej $username got ${tweets.size} tweets.")
      }

      tweets = tweet :: tweets
    }
    case (follow: FollowUser, sender: Sender) => {
      logger.info(s"$username now following: ${follow.name}")
      followers = follow.name :: followers
      sender ! TwitterSuccess()
    }
    case (f: GetFollowing, sender: Sender) =>
      sender ! FollowList(followers.toList)
    case (t: GetTweetListSize, sender: Sender) =>
      sender ! TweetListSize(tweets.size)
  }
}
