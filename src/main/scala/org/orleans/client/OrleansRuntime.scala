package main.scala.org.orleans.client
import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.{Grain, GrainRef}
import org.orleans.silo.control.{
  CreateGrainRequest,
  CreateGrainResponse,
  SearchGrainRequest,
  SearchGrainResponse
}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.{ClassTag, classTag}
import scala.util.{Failure, Success}

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

  val MASTER_ID: String = "master"
  val master: GrainRef = GrainRef(MASTER_ID, host, port)

  def createGrain[G <: Grain: ClassTag](): Future[GrainRef] = {
    val tag = classTag[G]
    (master ? CreateGrainRequest(tag)).flatMap {
      case value: CreateGrainResponse =>
        Future.successful(GrainRef(value.id, value.address, value.port))
      case _ =>
        Future.failed[GrainRef](
          new RuntimeException("Creating a grain failed."))
    }
  }
  def getGrain[G <: Grain: ClassTag](id: String): Future[GrainRef] = {
    val tag = classTag[G]
    (master ? SearchGrainRequest(id)).flatMap {
      case value: SearchGrainResponse =>
        Future.successful(GrainRef(id, value.address, value.port))
      case _ =>
        Future.failed[GrainRef](
          new RuntimeException(s"Search grain ${id} failed."))
    }
  }

  def getHost() = host
  def getPort() = port
}
