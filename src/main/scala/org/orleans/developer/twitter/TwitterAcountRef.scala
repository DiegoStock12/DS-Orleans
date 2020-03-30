package org.orleans.developer.twitter
import org.orleans.client.OrleansRuntime
import org.orleans.developer.twitter.TwitterMessages._
import org.orleans.silo.Services.Grain.{Grain, GrainRef, GrainReference}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class TwitterAcountRef extends GrainReference {

  def tweet(str: String): Unit =
    this.grainRef ! Tweet(str, System.currentTimeMillis().toString)

  def followUser(twitter: TwitterRef,
                 username: String): Future[TwitterSuccess] = {
    val userExists = (twitter.grainRef ? UserExists(username))
    userExists flatMap {
      case TwitterSuccess() => {
        this.grainRef ! FollowUser(username)
        Future { TwitterSuccess() }
      }
      case TwitterFailure(msg) =>
        Future.failed(new IllegalArgumentException(s"$username: $msg"))
    }
  }

}
