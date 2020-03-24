package org.orleans.silo.metrics

object MetricsExtractor {
  /**
   * Gets the number of pending requests in rpc service queue.
   *
   * @param registry Registry that collects metric.
   * @return Number of pending requests.
   */
  def getPendingRequests(registry: Registry): Int = {
    val started = registry.requestsReceived
    val handled = registry.requestsHandled
    started - handled
  }

  /**
   * Gets the number of Requests Per Second for the last time interval.
   *
   * @param registry Registry that collects metric.
   * @return RPS value.
   */
  def getRPS(registry: Registry): Unit = {
    //TODO If needed we can try to incorporate timestamps and calcualte RPS value for the time window
  }

}
