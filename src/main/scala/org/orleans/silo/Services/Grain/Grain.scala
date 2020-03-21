package org.orleans.silo.Services.Grain

abstract class Grain(val _id: String) extends Serializable {
  def store()
}
