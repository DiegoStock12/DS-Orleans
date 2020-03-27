package org.orleans.developer.twitter

import com.typesafe.scalalogging.LazyLogging
import org.orleans.developer.twitter.TwitterMessages
import org.orleans.developer.twitter.TwitterMessages.Tweet
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.Services.Grain.Grain.Receive

class TwitterAccount(id: String) extends Grain(id) with LazyLogging {

  private var username: String = ""
  private var tweets: List[Tweet] = List()

  override def receive: Receive = {
    case (tweet: Tweet, _) => tweets = tweet :: tweets
  }
}
