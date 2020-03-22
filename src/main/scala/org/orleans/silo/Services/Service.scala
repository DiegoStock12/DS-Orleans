package org.orleans.silo.Services

/**
  * Enumeration of all grpc services
  */
object Service {
  val ActivateGrain = "ACTIVATE_GRAIN"
  val GrainSearch = "GRAIN_SEARCH"
  val GrainStatusUpdate = "GRAIN_STATUS_UPDATE"
  val CreateGrain = "CREATE_GRAIN"

}
