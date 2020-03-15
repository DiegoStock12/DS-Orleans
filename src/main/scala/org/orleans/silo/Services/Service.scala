package org.orleans.silo.Services

import org.orleans.silo.utils.GrainState.Value

/**
 * Enumeration of all grpc services
 */
object Service extends Enumeration {
  type Service = Value
  val ActivateGrain, GrainSearch = Value
}
