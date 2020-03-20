package org.orleans.silo.storage

import ch.qos.logback.classic.{Level, LoggerContext}
import com.typesafe.scalalogging.LazyLogging
import org.json4s.jackson.Serialization
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.FindOneAndUpdateOptions
import org.mongodb.scala.{MongoClient, _}
import org.orleans.silo.Services.Grain.Grain
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success}

class TestGrain(id: String, val someField: String) extends Grain(id) {
  override def store(): Unit = {}

  override def toString = s"TestGrain($id, $someField)"
}

object DatabaseConnectionTest {

  def main(args: Array[String]): Unit = {
    val grain = new TestGrain("104", "testtesttest")

    MongoDatabase.store(grain)

    Thread.sleep(3000)

    val result = MongoDatabase.load[TestGrain]("104")
    result.onComplete {
      case Success(value) =>
        println("Success - value: " + value)
      case Failure(exception) =>
        println("Failure")
        println(exception)
    }

    Await.result(result, 10 seconds)

    Thread.sleep(10000)
    MongoDatabase.close()
  }

}

object MongoDatabase extends GrainDatabase with LazyLogging {

  private val connectionString: String = "mongodb://ds-orleans:SaFBNMjzP9CMLt@167.172.42.150:27017/grains"
  private val client = MongoClient(connectionString)
  private val database: MongoDatabase = client.getDatabase("grains")
  private val grainCollection: MongoCollection[Document] = database.getCollection("grain")

  // Set the log level for mongodb to ERROR
//  LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].getLogger("org.mongodb.driver").setLevel(Level.ERROR)

  override def store[T <: Grain with AnyRef : ClassTag : TypeTag](grain: T): Unit = {
    implicit val format: Formats = DefaultFormats + FieldSerializer[T]()
    val jsonString = Serialization.write(grain)(format)

    grainCollection.findOneAndUpdate(equal("id", grain.id), Document(jsonString), FindOneAndUpdateOptions().upsert(true))
  }

  override def load[T <: Grain with AnyRef : ClassTag : TypeTag](id: String): Future[T] = Future {
    implicit val format: Formats = DefaultFormats + FieldSerializer[T]()
    val observable: SingleObservable[Document] = grainCollection.find(equal("id", id)).first()

    val document = Await.result(observable.toFuture(), 10 seconds)

    Serialization.read[T](document.toJson())
  }

  override def load[T <: Grain with AnyRef : ClassTag : TypeTag](fieldName: String, value: AnyVal): Future[T] = Future[T] {
    implicit val format: Formats = DefaultFormats + FieldSerializer[T]()
    val observable: SingleObservable[Document] = grainCollection.find(equal(fieldName, value)).first()

    val document = Await.result(observable.toFuture(), 10 seconds)

    Serialization.read[T](document.toJson())
  }

  def close(): Unit = {
    client.close()
  }
}

