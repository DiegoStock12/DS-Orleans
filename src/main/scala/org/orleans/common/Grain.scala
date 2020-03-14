package org.orleans.common

trait Grain extends Serializable {
  def store()

}
