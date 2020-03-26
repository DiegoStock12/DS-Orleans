package main.scala.org.orleans.client
import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.{Grain, GrainRef}

import scala.collection.mutable
import scala.concurrent.Future
import scala.reflect.{ClassTag, classTag}

object OrleansRuntime {
  def apply(): OrleansRuntimeBuilder = new OrleansRuntimeBuilder()
}

class OrleansRuntimeBuilder extends LazyLogging {
  private var _host: String = "localhost"
  private var _port: Int = 0
  private var _grains: mutable.MutableList[ClassTag[_ <: Grain]] =
    mutable.MutableList()

  def setHost(hostString: String): OrleansRuntimeBuilder = {
    this._host = hostString
    return this
  }

  def setPort(portInt: Int): OrleansRuntimeBuilder = {
    this._port = portInt
    return this
  }

  def registerGrain[grain <: Grain: ClassTag](): OrleansRuntimeBuilder = {
    val tag = classTag[grain]

    if (this._grains.contains(tag)) {
      logger.warn(s"${tag.runtimeClass.getName} already registered in master.")
    }

    this._grains += classTag[grain]
    this
  }

  def build(): OrleansRuntime = {
    return new OrleansRuntime(_host, _port, this._grains.toList)
  }

}

class OrleansRuntime(private val host: String,
                     private val port: Int,
                     private val registeredGrains: List[ClassTag[_ <: Grain]] =
                       List()) {

  def createGrain[G <: Grain: ClassTag](): Future[GrainRef] = {
    //TODO So here the master is request to create a grain, which will return a grainref which can be used to manipulate it.

    null
  }
  def getGrain[G <: Grain: ClassTag](id: String): Future[GrainRef] = {
    //TODO So here the master is asked to find a grain (either in memory) or persistent storage.
    null
  }

  def getHost() = host
  def getPort() = port
}
