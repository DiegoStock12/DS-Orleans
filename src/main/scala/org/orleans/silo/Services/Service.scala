package org.orleans.silo.Services

/**
 * Enumeration of all grpc services
 */
object Service extends Enumeration {
  type Service = Value
  // Include the name of the service so we can reflect
  val ActivateGrain = Value("ActivateGrain")
  val GrainSearch = Value("GrainSearch")
  val Hello = Value("Greeter")
  val CreateGrain = Value("CreateGrain")
}
