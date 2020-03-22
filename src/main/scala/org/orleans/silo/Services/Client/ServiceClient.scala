package org.orleans.silo.Services.Client
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import io.grpc.ManagedChannel

abstract class ServiceClient[T](var managedChannel: ManagedChannel,
                                val stub: T) {

  def shutdown(): Unit = {
    managedChannel.shutdown.awaitTermination(5, TimeUnit.MILLISECONDS)
  }

  def invalidateGrain(): Unit = {}

}
