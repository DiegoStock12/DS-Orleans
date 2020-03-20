package org.orleans.silo.Services.Grain

abstract class Grain extends Serializable {
  var id = "test"
  def store();
}
