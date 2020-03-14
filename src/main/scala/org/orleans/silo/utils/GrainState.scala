package org.orleans.silo.utils

object GrainState extends Enumeration {
  type GrainState = Value
  val InMemory, Persisted, Activating, Deactivating = Value
}
