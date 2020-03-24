package org.orleans.silo.metrics

object RegistryFactory{

    // Active registries collecting metrics on different services.
    private var registries: Map[String, Registry] = Map[String, Registry]()

    /**
     * Creats and returns registry for new service.
     * @param id Id of the service.
     * @return New registry.
     */
    def getOrCreateRegistry(id: String): Registry = {
      registries.getOrElse(id, {
        val newRegistry = new Registry()
        registries = registries + (id -> newRegistry)
        newRegistry
      })
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
    def getRegistries(): Map[String, Registry] = {
        registries
    }


    /**
     * Gets the loads per service id.
     * @return Map of loads per service.
     */
    def getRegistryLoads(): Map[String, Int] = {
        registries map {case (id, registry) => (id, MetricsExtractor.getPendingRequests(registry))}
    }

}
