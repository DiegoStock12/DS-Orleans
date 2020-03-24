package org.orleans.silo.Services.Grain

import org.orleans.silo.storage.{GrainDatabase, MongoDatabase}

import scala.concurrent.Future

object Grain {
  type Receive = PartialFunction[Any, Unit]
  lazy val database: GrainDatabase = MongoDatabase
}

abstract class Grain(val _id: String) extends Serializable {
  def receive: Grain.Receive

  def store(): Future[Option[Grain]] = {
    Grain.database.store(this)
  }
}
