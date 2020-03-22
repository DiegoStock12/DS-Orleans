package main.scala.org.orleans.client
import org.orleans.developer.TwitterAccountClient
import org.orleans.silo.Services.Client.{
  CreateGrainClient,
  SearchServiceClient,
  ServiceClient,
  ServiceFactory
}
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.createGrain.CreationResponse
import org.orleans.silo.grainSearch.{SearchRequest, SearchResult}
import scalapb.grpc.AbstractService

import collection.mutable
import scala.concurrent.Future
import scala.reflect.{ClassTag, _}
import scala.reflect.runtime.universe._

object OrleansRuntimeBuilder {
  def apply(): OrleansRuntimeBuilder = new OrleansRuntimeBuilder()
}

class OrleansRuntimeBuilder {
  private var _host: String = "localhost"
  private var _port: Int = 0
  private var _grains: mutable.Map[ClassTag[_], (ClassTag[_], ClassTag[_])] =
    mutable.Map()

  def host(hostString: String): OrleansRuntimeBuilder = {
    this._host = hostString
    return this
  }

  def port(portInt: Int): OrleansRuntimeBuilder = {
    this._port = portInt
    return this
  }

  def registerGrain[grain <: Grain: TypeTag: ClassTag,
                    client <: ServiceClient[_]: TypeTag: ClassTag,
                    grpc <: AbstractService with AnyRef: ClassTag: TypeTag]()
    : OrleansRuntimeBuilder = {
    this._grains
      .put(classTag[grain], (classTag[client], classTag[grpc]))

    return this
  }

  def build(): OrleansRuntime = {
    //TODO Here we should check somehow if the grain is also registered on a silo?
    return new OrleansRuntime(_host, _port, this._grains.toMap)
  }

}

object OrleansRuntime {
  def apply(master_host: String, master_port: Int): OrleansRuntime =
    new OrleansRuntime(master_host, master_port)
}

class OrleansRuntime(
    private val master_host: String,
    private val master_port: Int,
    private val registeredGrains: Map[ClassTag[_], (ClassTag[_], ClassTag[_])] =
      Map()) {

  def createGrain[G <: Grain: ClassTag: TypeTag](): Future[CreationResponse] = {
    val createGrainService = ServiceFactory.getService[CreateGrainClient](this)

    val tags = registeredGrains.get(classTag[G])

    if (tags.isEmpty) {
      //TODO Cannot find grain, throw error.
    }
    //magic hereeee

    createGrainService.createGrain()(tags.get._2, classTag[G])
  }

  def getGrain[G <: Grain: ClassTag](id: String): ServiceClient[G] = {
    val createGrainService =
      ServiceFactory.getService[SearchServiceClient](this)

    val tags = registeredGrains.get(classTag[G])

    if (tags.isEmpty) {
      //TODO Cannot find grain, throw error.
    }

    // //Search the grain!
    createGrainService.search(id)

    //finally link it to the client
    ServiceFactory.getService(this)(tags.get._1).asInstanceOf[ServiceClient[G]]
  }

  def getHost() = master_host
  def getPort() = master_port
}
