package org.orleans.silo.utils

import io.prometheus.client.CollectorRegistry

object RegistryFactory{

    // Active registries collecting metrics on different services.
    private var registries: Map[String, CollectorRegistry] = Map[String, CollectorRegistry]()

    /**
     * Creats and returns registry for new service.
     * @param id Id of the service.
     * @return New registry.
     */
    def getRegistry(id: String): CollectorRegistry = {
        val newRegistry = new CollectorRegistry()
        registries = registries + (id -> newRegistry)
        newRegistry
    }

    /**
     * Deletes the registry of the service.
     * @param id Id of the service.
     */
    def deleteRegistry(id: String): Unit = {
        registries = registries - id
    }

    /**
     * Gets the collection of all active registries
     * @return Map of registries per service.
     */
    def getRegistries(): Map[String, CollectorRegistry] = {
        registries
    }


    /**
     * Gets the loads per service id.
     * @return Map of loads per service.
     */
    def getRegistryLoads(): Map[String, Double] = {
        registries map {case (id, registry) => (id, MetricsExtractor.getPendingRequests(registry))}
    }

}


