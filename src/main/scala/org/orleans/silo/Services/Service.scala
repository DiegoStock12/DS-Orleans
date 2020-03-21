package org.orleans.silo.Services

/**
 * Enumeration of all grpc services
 */
object Service extends Enumeration {
  type Service = Value
  val ActivateGrain, GrainSearch , Hello, GrainStatusUpdate, CreateGrain = Value

}
