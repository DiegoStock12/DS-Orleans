package org.orleans.silo.Test

import org.orleans.silo.Services.Client.{CreateGrainClient, ServiceFactory}
import org.orleans.silo.Services.Grain.GrainFactory
import org.orleans.silo.Services.Service

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object Testing {
  // Just a test for the new Service client
  def main(args: Array[String]): Unit = {
    // get the serviceClientFactory with localhost and default port as parameters
    //    val scf = GrainFactory("localhost")
    val client = ServiceFactory
      .getService(Service.CreateGrain, "localhost", 50050)
      .asInstanceOf[CreateGrainClient]
    val f = client.createGrain(Service.Hello.id, Service.Hello.toString)
    f onComplete{
      case Success(res) => println(res)
      case Failure(e) => e.printStackTrace()
    }

    Thread.sleep(15000)
    // Get the Future for the Stub of the GreeterService
    //    val f = scf.getGrain(id = "diegoalbo").asInstanceOf[Future[GreeterClient]]
    //    println("Waiting for the result")
    //    f onComplete{
    //      case Success(client) =>
    //        client.greet("Diego!!")
    //      case Failure(exception) => exception.printStackTrace()
    //    }
    //
    //    Thread.sleep(10000)

  }
}
