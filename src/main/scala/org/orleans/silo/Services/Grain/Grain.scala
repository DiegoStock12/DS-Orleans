package org.orleans.silo.Services.Grain

import org.orleans.silo.storage.{GrainDatabase, MongoDatabase}

import scala.concurrent.Future

object Grain {
  lazy val database: GrainDatabase = MongoDatabase
}

abstract class Grain(val _id: String) extends Serializable {
  def store(): Future[Option[Grain]] = {
    Grain.database.store(this)
  }
}
