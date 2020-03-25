package org.orleans.silo.Test

import org.orleans.silo.Services.Grain.GrainRef
import org.orleans.silo.control.CreateGrainRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import scala.reflect._

object Testing {
  // Just a test for the new Service client
  def main(args: Array[String]): Unit = {
    println("Trying to get the socket")

    // Get the grain reference
    val g = GrainRef("892c7fff-4140-4cc0-98e8-8d60851c3a02", "localhost", 1400)
    println("Sending request")
    val tag = classTag[GreeterGrain]
    println(tag)
    g ? CreateGrainRequest(tag) onComplete{
      case Success(value) => println(value)
      case _ => println("not working")
    }

    Thread.sleep(5000)



    // Send a message to the grain
    // Synchronous request
//    println("Sending hello message to grain 1234")
//    time {
//      g ! "hello"
//      g ! "hello"
//    }
//
//
//    // Asynchronous request
//    time {
//      g ? 2 onComplete ({
//        case Success(value) => println(value)
//        case _ => println("not working")
//      })
//    }

    Thread.sleep(1500)
  }

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) / 1e6 + "ms")
    result
  }
}
