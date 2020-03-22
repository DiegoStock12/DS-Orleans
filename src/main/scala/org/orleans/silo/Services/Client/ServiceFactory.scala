package org.orleans.silo.Services.Client

import scala.reflect.runtime.universe._
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import main.scala.org.orleans.client.OrleansRuntime
import org.orleans.developer.TwitterAccountClient
import org.orleans.silo.Services.Service
import org.orleans.silo.Test.GreeterClient
import org.orleans.silo.activateGrain.ActivateGrainServiceGrpc
import org.orleans.silo.createGrain.CreateGrainGrpc
import org.orleans.silo.grainSearch.GrainSearchGrpc
import org.orleans.silo.hello.GreeterGrpc

import scala.reflect.ClassTag
import scala.reflect._
import scala.reflect.runtime.universe._

/**
  * Factory for getting the client to particular service
  */
object ServiceFactory {

  /**
    * Gets the desired service
    *
    * @param service service type from the defined ones
    */
  def getService[T: ClassTag](runtime: OrleansRuntime,
                              stubType: String = "async"): T = {
    val c = ManagedChannelBuilder
      .forAddress(runtime.getHost(), runtime.getPort())
      .usePlaintext()
      .build()

    val tag = classTag[T]
    tag match {
      case x if x == classTag[ActivateGrainClient] =>
        new ActivateGrainClient(c).asInstanceOf[T]
      case x if x == classTag[SearchServiceClient] =>
        new SearchServiceClient(c).asInstanceOf[T]
      case x if x == classTag[CreateGrainClient] =>
        new CreateGrainClient(c, stubType).asInstanceOf[T]
      case _ => {
        // Create an instance of the client
        val clientClass = tag
        val clientInstance = clientClass.runtimeClass
          .getConstructor(classOf[ManagedChannel])
          .newInstance(c)

        return clientInstance.asInstanceOf[T]
      }
    }
  }
  implicit class MyInstanceOf[U: TypeTag](that: U) {
    def myIsInstanceOf[T: TypeTag] =
      typeOf[U] <:< typeOf[T]
  }
}
