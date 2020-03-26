package org.orleans.silo.storage

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.Services.Grain.Grain.Receive

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.{Failure, Success}

class TestGrain(_id: String) extends Grain(_id) {
  val someField: String = "testtest"

  override def toString = s"TestGrain(${_id}, $someField)"
  override def receive: Receive = ???
}

object DatabaseConnectionTest extends LazyLogging {



  import scala.concurrent.ExecutionContext.Implicits.global

  def main(args: Array[String]): Unit = {
    val grain = new TestGrain("1013")

    val mongo = new MongoGrainDatabase("test")

    val storeResult = mongo.store(grain)
    storeResult.onComplete {
      case Success(value) =>
        if (value.isEmpty) {
          logger.debug(s"Succesfully stored new grain.")
        } else {
          logger.debug(s"Succesfully stored grain. Old value of grain: $value")
      }

      case Failure(e) =>
        logger.error(s"Something went wrong during storing of grain. Cause: ${e.toString}")
    }

    Await.ready(storeResult, 10 seconds)

    val result = mongo.load[TestGrain]("1012")
    result.onComplete {
      case Success(value) =>
        logger.debug(s"Succesfully retrieved grain: $value")
      case Failure(e) =>
        logger.error(s"Something went wrong during loading of grain. Cause: $e")
    }

    Await.ready(result, 10 seconds)

    mongo.close()
  }

}