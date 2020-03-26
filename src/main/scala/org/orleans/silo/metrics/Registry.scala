package org.orleans.silo.metrics

/**
 * Registry for collecting information about requests toi grain.
 */
class Registry() {
  @volatile
  var requestsReceived: Int = 0
  @volatile
  var requestsHandled: Int = 0

  /**
   * Increase the counter of requests received.
   */
  def addRequestReceived(): Unit = {
    requestsReceived += 1
  }

  /**
   * Increase the counter of requests processed.
   */
  def addRequestHandled(): Unit = {
    requestsHandled += 1
  }
}
