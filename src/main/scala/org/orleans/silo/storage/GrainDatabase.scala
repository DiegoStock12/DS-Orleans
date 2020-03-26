package org.orleans.silo.storage

import org.orleans.silo.Services.Grain.Grain

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object GrainDatabase {
  lazy val instance: GrainDatabase = MongoDatabase
}

trait GrainDatabase {

  def store[T <: Grain with AnyRef : ClassTag : TypeTag](grain: T): Future[Option[T]]
  def load[T <: Grain with AnyRef : ClassTag : TypeTag](id: String): Future[T]
  def load[T <: Grain with AnyRef : ClassTag : TypeTag](fieldName: String, value: Any): Future[T]
  def delete[T <: Grain with AnyRef : ClassTag : TypeTag](id: String): Future[T]

}
