package org.orleans.silo.Services.Grain

trait Grain extends Serializable {
  def store()
}
