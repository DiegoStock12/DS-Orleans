package main.scala.org.orleans.client
import org.orleans.silo.Services.Client.ServiceClient
import org.orleans.silo.Services.Grain.Grain

class OrleansRuntime(master_host: String, master_port: Int) {

  def registerGrain[G <: Grain, GS <: ServiceClient]() = {}

}
