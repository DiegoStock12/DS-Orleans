package org.orleans.silo.storage

import org.orleans.common.Grain

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

trait GrainDatabase {

  def store[T <: Grain with AnyRef : ClassTag : TypeTag](grain: T): Unit
  def load[T <: Grain with AnyRef : ClassTag : TypeTag](id: String): Future[T]
  def load[T <: Grain with AnyRef : ClassTag : TypeTag](fieldName: String, value: AnyVal): Future[T]



}
