package main.scala.org.orleans.client
import com.typesafe.scalalogging.LazyLogging
import org.orleans.developer.twitter.TwitterAcountRef
import org.orleans.silo.Services.Grain.{Grain, GrainRef, GrainReference}
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
object OrleansRuntime {
  def apply(): OrleansRuntimeBuilder = new OrleansRuntimeBuilder()

  def createGrain[G <: Grain: ClassTag, Ref <: GrainReference: ClassTag](
      master: GrainRef): Future[Ref] = {
    val tagGrain = classTag[G]
    val tagRef = classTag[Ref]
    (master ? CreateGrainRequest(tagGrain)).flatMap {
      case value: CreateGrainResponse => {
        val id = value.id
        val address = value.address
        val port = value.port
        val grainRef = GrainRef(id, address, port)

        val ref: Ref = tagRef.runtimeClass
          .getDeclaredConstructor()
          .newInstance()
          .asInstanceOf[Ref]
        ref.setGrainRef(grainRef)
        ref.setMasterGrain(master)
        Future.successful(ref)
      }
      case _ =>
        Future.failed[Ref](new RuntimeException("Creating a grain failed."))
    }
  }

  def getGrain[G <: Grain: ClassTag, Ref <: GrainReference: ClassTag](
      id: String,
      master: GrainRef): Future[Ref] = {
    val tag = classTag[G]
    val tagRef = classTag[Ref]
    (master ? SearchGrainRequest(id)).flatMap {
      case value: SearchGrainResponse => {
        val address = value.address
        val port = value.port
        val grainRef = GrainRef(id, address, port)

        val ref: Ref = tagRef.runtimeClass
          .getDeclaredConstructor()
          .newInstance()
          .asInstanceOf[Ref]
        ref.setGrainRef(grainRef)
        ref.setMasterGrain(master)
        Future.successful(ref)
      }
      case _ =>
        Future.failed[Ref](new RuntimeException(s"Search grain ${id} failed."))
    }
  }
}

class OrleansRuntime(private val host: String,
                     private val port: Int,
                     private val registeredGrains: List[ClassTag[_ <: Grain]] =
                       List()) {

  val MASTER_ID: String = "master"
  val master: GrainRef = GrainRef(MASTER_ID, host, port)

  def createGrain[G <: Grain: ClassTag, Ref <: GrainReference: ClassTag]()
    : Future[Ref] = OrleansRuntime.createGrain[G, Ref](master)

  def getGrain[G <: Grain: ClassTag, Ref <: GrainReference: ClassTag](
      id: String): Future[Ref] = OrleansRuntime.getGrain[G, Ref](id, master)

  def getHost() = host
  def getPort() = port
}
