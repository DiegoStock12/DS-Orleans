package org.orleans.silo.Test

import org.orleans.silo.Services.Grain.GrainRef
import org.orleans.silo.control.{CreateGrainRequest, CreateGrainResponse, DeleteGrainRequest, SearchGrainRequest, SearchGrainResponse}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.reflect.runtime.universe._
import scala.reflect._

object Testing {
  // Just a test for the new Service client
  def main(args: Array[String]): Unit = {
    println("Trying to get the socket")

    var id: String = ""

    val classtag = classTag[GreeterGrain]
    val typetag = typeTag[GreeterGrain]

    // The master grain in the master has ID "master" so it's easy to find!
    val g = GrainRef("master", "localhost", 1400)
    // Get the grain reference
//    val g = GrainRef("b01ed5f3-96eb-45ea-8eea-53e383e8c2f6", "localhost", 1400)
//    val g = GrainRef("1234", "localhost", 1400)

    // Try to search for a grain that is deactivated
    g ? SearchGrainRequest("29d95c86-b744-43cc-9de3-23a0e40c21c3", classtag, typetag) onComplete {
      case Success(value: SearchGrainResponse) =>
        println("Succesfully activate persistant grain by search!")
        println(value)
      case Success(value) =>
        println(s"Unknown return value received: $value!")
      case Failure(exception) => exception.printStackTrace()
    }
    Thread.sleep(1000)



    // Try to create a grain
    println("Creating the grain!")
    val result = g ? CreateGrainRequest(classtag, typetag)
    val mappedResult = result.map {
      case value: CreateGrainResponse =>
        println("Received CreateGrainResponse!")
        println(value)
        id = value.id
      case other => println(s"Something went wrong: $other")
    }
    Await.result(mappedResult, 5 seconds)

    println(s"ID of the grain is $id")
    println("Searching for the grain")

    var port : Int = 0

    // Search for a grain
    g ? SearchGrainRequest(id, classtag, typetag) onComplete {
      case Success(value: SearchGrainResponse) =>
        println(value)
        port = value.port
      case Failure(exception) => exception.printStackTrace()
    }
    Thread.sleep(1000)

    println("Sending hello to the greeter grain")
    val g2 : GrainRef = GrainRef(id, "localhost", port)
    g2 ? "hello" onComplete{
      case Success(value) => println(value)
      case _ => "Not working"
    }

    Thread.sleep(5000)

    // Delete that grain
    println("Trying to delete grain")
    // Try to delete the grain
    g ! DeleteGrainRequest(id)

  }

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) / 1e6 + "ms")
    result
  }
}
