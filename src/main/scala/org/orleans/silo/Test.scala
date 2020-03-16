package org.orleans.silo

import org.orleans.silo.Services.Client.{GreeterClient, ServiceClientFactory}
import org.orleans.silo.Services.Service

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Test {
  // Just a test for the new Service client
  def main(args: Array[String]): Unit = {
    // get the serviceClientFactory with localhost and default port as parameters
    val scf = ServiceClientFactory("localhost")
    // Get the Future for the Stub of the GreeterService
    val f = scf.getGrain(Service.Hello, id = "diegoalbo").asInstanceOf[Future[GreeterClient]]
    println("Waiting for the result")
    f onComplete{
      case Success(client) =>
        client.greet("Diego!!")
      case Failure(exception) => exception.printStackTrace()
    }

    Thread.sleep(10000)

  }
}
