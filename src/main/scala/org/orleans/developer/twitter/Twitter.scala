package org.orleans.developer.twitter
import org.orleans.developer.twitter.TwitterMessages._
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.Services.Grain.Grain.Receive
import org.orleans.silo.dispatcher.Sender

class Twitter(id: String) extends Grain(id) {

  private var accounts: Map[String, String] = Map()

  override def receive: Receive = {
    case (user: UserExists, sender: Sender) => {
      if (accounts
            .get(user.username)
            .isEmpty) {
        sender ! Success()
      } else {
        println("Lol this user already exists")
        sender ! Failure("User already exists.")
      }
    }
    case (user: UserCreate, _) => //Add user here
      println(s"User is added ${user.username}")
      accounts = Map(user.username -> user.ref) ++ accounts
    case (user: UserGet, sender: Sender) => {
      if (accounts.get(user.username).isDefined) {
        sender ! UserRetrieve(accounts.get(user.username).get)
      } else {
        sender ! Failure("Username doesn't exist.")
      }
    }
  }
}
