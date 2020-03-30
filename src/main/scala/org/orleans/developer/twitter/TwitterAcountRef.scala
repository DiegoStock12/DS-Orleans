package org.orleans.developer.twitter
import org.orleans.client.OrleansRuntime
import org.orleans.developer.twitter.TwitterMessages._
import org.orleans.silo.Services.Grain.{Grain, GrainRef, GrainReference}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

class TwitterAcountRef extends GrainReference {
  def tweet(str: String) =
    (this.grainRef ! Tweet(str, System.currentTimeMillis().toString))

  def followUser(twitter: TwitterRef, username: String): Future[Any] = {
    val userExists = (twitter.grainRef ? UserExists(username))
    userExists flatMap {
      case TwitterFailure("User already exists.") =>
        this.grainRef ? FollowUser(username)
      case x => throw new IllegalArgumentException(s"here $x")
    }
  }

  def getFollowingList(): Future[List[String]] = {
    (this.grainRef ? GetFollowing()) map {
      case FollowList(followers: List[String]) => {
        followers
      }
      case x => throw new IllegalArgumentException(x.toString)
    }
  }

  def getAmountOfTweets(): Future[Int] = {
    (this.grainRef ? GetTweetListSize()) map {
      case TweetListSize(tweetSize: Int) => tweetSize
      case x                             => throw new IllegalArgumentException(x.toString)
    }
  }

}
