package org.orleans.silo.control

import org.orleans.silo.Services.Grain.Grain

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

case class GrainType[T <: Grain](classTag: ClassTag[T], typeTag: TypeTag[T])
