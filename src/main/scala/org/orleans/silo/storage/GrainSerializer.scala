package org.orleans.silo.storage

import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.orleans.silo.Services.Grain.Grain

import scala.reflect.runtime.universe._
import scala.reflect.ClassTag
import org.json4s.jackson.Serialization

object GrainSerializer {

  def serialize[T <: Grain with AnyRef : ClassTag : TypeTag](grain: T): String = {
    implicit val format: Formats = DefaultFormats + FieldSerializer[T]()

    Serialization.write(grain)(format)
  }

  def deserialize[T <: Grain with AnyRef : ClassTag : TypeTag](jsonString: String): T = {
    implicit val format: Formats = DefaultFormats + FieldSerializer[T]()

    Serialization.read[T](jsonString)
  }

}
