package org.orleans.developer.twitter
import main.scala.org.orleans.client.OrleansRuntime
import org.orleans.developer.twitter.TwitterMessages._
import org.orleans.silo.Services.Grain.{GrainRef, GrainReference}

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class TwitterRef extends GrainReference {
  def createAccount(username: String): Future[_] = {
    // First we try check if the username already exists,
    // then if not we create a new grain of type TwitterAccount.
    val userExists = (grainRef ? UserExists(username))
    val userFlatmap = userExists.flatMap {
      case TwitterSuccess() => {
        val grain = OrleansRuntime
          .createGrain[TwitterAccount, TwitterAcountRef](masterRef)

        grain flatMap {
          case x: TwitterAcountRef =>
            (grainRef ? UserCreate(username, x.grainRef.id)).flatMap {
              case TwitterSuccess() => grain
            }
        } andThen {
          case _ => grain
        }
      }
      case TwitterFailure(msg) =>
        Future.failed(new IllegalArgumentException(s"$username: $msg"))
    }
    userFlatmap
  }

  def getAccount(username: String): Future[Try[TwitterAcountRef]] = {
    (grainRef ? UserGet(username)).flatMap {
      case UserRetrieve(grainId: String) => {
        mapValue(
          OrleansRuntime
            .getGrain[TwitterAccount, TwitterAcountRef](grainId, masterRef)
        )
      }
      case TwitterFailure(msg: String) =>
        Future.failed(new IllegalArgumentException(s"$username: $msg"))
    }
  }

  private def mapValue[T](f: Future[T]): Future[Try[T]] = {
    val prom = Promise[Try[T]]()
    f onComplete prom.success
    prom.future
  }

}
