package org.orleans.silo.storage

import ch.qos.logback.classic.{Level, LoggerContext}
import com.typesafe.scalalogging.LazyLogging
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.FindOneAndUpdateOptions
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.Services.Grain.Grain.Receive
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success}

object MongoDatabase extends GrainDatabase with LazyLogging {
  // Set the log level for mongodb to ERROR
  LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].getLogger("org.mongodb.driver").setLevel(Level.ERROR)

  private val connectionString: String = "mongodb://ds-orleans:SaFBNMjzP9CMLt@167.172.42.150:27017/"
  lazy private val client = MongoClient(connectionString)
  lazy private val database: MongoDatabase = client.getDatabase("grains")
  lazy private val grainCollection: MongoCollection[Document] = database.getCollection("grain")

  def setMongoLogLevel(level: Level): Unit = {
    LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].getLogger("org.mongodb.driver").setLevel(level)
  }

  /**
    * Stores a grain to persistant storage. In case the grain is already stored (based on the _id field) it will overwrite it.
    * @param grain Grain to be stored
    * @tparam T Specific subtype of the grain
    * @return Returns a Future that contains the old stored grain if it was successfully stored or else an exception
    */
  override def store[T <: Grain with AnyRef : ClassTag : TypeTag](grain: T): Future[Option[T]] = {
    val jsonString = GrainSerializer.serialize(grain)
    logger.debug(s"Inserting or updating grain: $grain")
    val result = grainCollection.findOneAndUpdate(equal("_id", grain._id), Document("$set" -> Document(jsonString)), FindOneAndUpdateOptions().upsert(true))

    result.toFuture().transform {
      case Success(document) =>
        logger.debug("Succesfully stored grain! Now deserializing...")
        if (document != null) {
          logger.debug(document.toJson())
          Success(Some(GrainSerializer.deserialize[T](document.toJson())))
        } else {
          Success(None)
        }

      case Failure(e) =>
        logger.debug("Something went wrong when storing the grain.")
        Failure(e)
    }
  }

  /**
    * Loads a grain based on its id
    * @param id Grain id.
    * @tparam T Type of the grain that is being loaded.
    * @return The loaded grain
    */
  override def load[T <: Grain with AnyRef : ClassTag : TypeTag](id: String): Future[T] = {
    load("_id", id)
  }

  /**
    * Loads a grain based on its id
    * @param fieldName Name of the field to search on.
    * @param value Value that needs to be found
    * @tparam T Type of the grain that is being loaded.
    * @return The loaded grain
    */
  override def load[T <: Grain with AnyRef : ClassTag : TypeTag](fieldName: String, value: Any): Future[T] = {
    val observable: SingleObservable[Document] = grainCollection.find(equal(fieldName, value)).first()

    observable.toFuture().transform {
      case Success(document) =>
        logger.debug(s"Succesfully loaded grain: ${document.toJson()}! Now deserializing...")
        Success(GrainSerializer.deserialize(document.toJson()))
      case Failure(e) =>
        logger.debug("Something went wrong when storing the grain.")
        Failure(e)
    }
  }

  /**
    * Closes the connection with the database.
    */
  def close(): Unit = {
    logger.debug("Closing the connection with mongodb.")
    client.close()
  }
}

