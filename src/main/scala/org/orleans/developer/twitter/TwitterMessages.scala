package org.orleans.developer.twitter
import org.orleans.silo.Services.Grain.GrainRef

object TwitterMessages {

  case class UserExists(username: String)
  case class UserCreate(username: String, ref: String)
  case class UserGet(username: String)
  case class UserRetrieve(grainId: String)

  case class Tweet(msg: String, timestamp: String)
  case class FollowUser(id: String)

  case class TwitterSuccess()
  case class TwitterFailure(failure: String)

}
