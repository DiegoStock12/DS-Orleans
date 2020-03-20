package org.orleans.silo.Services.Grain

abstract class Grain(val id: String) extends Serializable {
  def store()
}
