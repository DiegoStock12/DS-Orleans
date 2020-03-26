package org.orleans.developer.twitter
import main.scala.org.orleans.client.OrleansRuntime
import org.orleans.developer.twitter.TwitterMessages._
import org.orleans.silo.Services.Grain.{GrainRef, GrainReference}

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

class TwitterRef extends GrainReference {
  def createAccount(username: String): Future[Try[TwitterAcountRef]] = {
    // First we try check if the username already exists,
    // then if not we create a new grain of type TwitterAccount.
    (grainRef ? UserExists(username)).flatMap {
      case Success() => {
        val grain = OrleansRuntime
          .createGrain[TwitterAccount, TwitterAcountRef](masterRef)

        grain.onComplete {
          case scala.util.Success(ref: TwitterAcountRef) => {
            println("Now creating the user in the TwitterGrain.")
            grainRef ! UserCreate(username, ref.grainRef.id)
          }
          case scala.util.Failure(e) =>
            Future.failed(new IllegalArgumentException(e))
        }

        mapValue(grain)
      }
      case Failure(msg: String) => {
        println("HERE NOW")
        Future.failed(new IllegalArgumentException(msg))
      }
    }
  }

  def getAccount(username: String): Future[Try[TwitterAcountRef]] = {
    (grainRef ? UserGet(username)).flatMap {
      case UserRetrieve(grainId: String) => {
        mapValue(
          OrleansRuntime
            .getGrain[TwitterAccount, TwitterAcountRef](grainId, masterRef)
        )
      }
      case Failure(msg: String) =>
        Future.failed(new IllegalArgumentException(msg))
    }
  }

  private def mapValue[T](f: Future[T]): Future[Try[T]] = {
    val prom = Promise[Try[T]]()
    f onComplete prom.success
    prom.future
  }

}
