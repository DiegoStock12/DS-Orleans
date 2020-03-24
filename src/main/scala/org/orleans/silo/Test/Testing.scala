package org.orleans.silo.Test

import org.orleans.silo.Services.Grain.GrainRef

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success


object Testing {
  // Just a test for the new Service client
  def main(args: Array[String]): Unit = {
    println("Trying to get the socket")

    // Get the grain reference
    val g = GrainRef("1234", "localhost", 2500)

    // Send a message to the grain
    // Synchronous request
    println("Sending hello message to grain 1234")
    time {
      g ! "hello"
      g ! "hello"
    }


    // Asynchronous request
    time {
      g ? 2 onComplete ({
        case Success(value) => println(value)
        case _ => println("not working")
      })
    }

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
