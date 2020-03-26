package org.orleans.client
import main.scala.org.orleans.client.OrleansRuntime
import org.orleans.silo.Services.Grain.GrainRef
import org.orleans.silo.Test.GreeterGrain

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object ClientMain {

  def main(args: Array[String]): Unit = {
    val runtime = OrleansRuntime()
      .registerGrain[GreeterGrain]
      .setHost("localhost")
      .setPort(1400)
      .build()

    var time = System.currentTimeMillis()
    val createGrainFuture: Future[GrainRef] =
      runtime.createGrain[GreeterGrain]()
    val grainRef = Await.result(createGrainFuture, 5 seconds)

    println(s"Creating a grain took ${System.currentTimeMillis() - time}ms")

    time = System.currentTimeMillis()
    val searchGrainFuture: Future[GrainRef] =
      runtime.getGrain[GreeterGrain](grainRef.id)
    val searchGrainRef = Await.result(searchGrainFuture, 5 seconds)

    println(s"Searching a grain took ${System.currentTimeMillis() - time}ms")
    println(s"GrainRefs are equal: ${grainRef == searchGrainRef}.")

  }
}
