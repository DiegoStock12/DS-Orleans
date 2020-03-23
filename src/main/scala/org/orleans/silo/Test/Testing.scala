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
    time {
      val f = client.createGrain[GreeterGrpc.Greeter, GreeterGrain]()
      Await.result(f, 4 seconds)
//      f onComplete {
//        case Success(res) => println(s"Success response $res")
//        case Failure(e) => e.printStackTrace()
//      }
    }
    time{
      val f = client.createGrain[GreeterGrpc.Greeter, GreeterGrain]()
      Await.result(f, 1 seconds)
//      f onComplete {
//        case Success(res) => println(s"Success response $res")
//        case Failure(e) => e.printStackTrace()
//      }
    }

    //Thread.sleep(15000)

  }

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0)/1e6 + "ms")
    result
  }
}
