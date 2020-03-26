package org.orleans.developer.twitter
import org.orleans.developer.twitter.TwitterMessages.Tweet
import org.orleans.silo.Services.Grain.{Grain, GrainRef, GrainReference}

class TwitterAcountRef extends GrainReference {
  def tweet(str: String): Unit =
    this.grainRef ! Tweet(str, System.currentTimeMillis().toString)

}
