package org.orleans.silo.utils

import io.prometheus.client.{Collector, CollectorRegistry}

object MetricsExtractor {

  /**
   * Gets the number of pending requests in rpc service queue.
   * @param collectorRegistry Registry that collects metric.
   * @return Number of pending requests.
   */
  def getPendingRequests(collectorRegistry: CollectorRegistry): Double = {
    val started = extractMetricValue("grpc_server_started_total", collectorRegistry)
    val handled = extractMetricValue("grpc_server_handled_total", collectorRegistry)
    started - handled
  }

  /**
   * Extracts the value of certain metric
   * @param name Name of the metric.
   * @param collectorRegistry Registry that collects metric.
   * @return Value of the certain metric.
   */
  def extractMetricValue(name: String, collectorRegistry: CollectorRegistry): Double = {
    val result = findRecordedMetric(name, collectorRegistry)
    if (result.samples.size > 1)
      throw new IllegalArgumentException(String.format(s"Expected one value, but got $result.samples.size for metric $name"))
    else if (result.samples.size() == 1)
      result.samples.get(0).value
    else
      0.0
  }

  /**
   * Find certain metric in the registry
   * @param name Name of the metric.
   * @param collectorRegistry Registry that collects metric.
   * @return Metric.
   */
  def findRecordedMetric(name: String, collectorRegistry: CollectorRegistry): Collector.MetricFamilySamples = {
    val samples = collectorRegistry.metricFamilySamples
    while (samples.hasMoreElements) {
      val sample = samples.nextElement
      if (sample.name == name)
        return sample
    }
    throw new IllegalArgumentException("Could not find metric with name: " + name)
  }
}
