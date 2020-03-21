package org.orleans.silo.Test

import org.orleans.silo.Services.Client.ServiceFactory
import org.orleans.silo.hello.GreeterGrpc

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Testing {
  // Just a test for the new Service client
  def main(args: Array[String]): Unit = {
    val client = ServiceFactory.createGrainService("localhost", 50050)
    val f = client.createGrain[GreeterGrpc.Greeter, GreeterGrain]()
    Await.result(f, 10 seconds)
    f onComplete {
      case Success(res) => println(s"Success response $res")
      case Failure(e)   => e.printStackTrace()
    }

    //Thread.sleep(15000)

  }
}
