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

    val time = System.currentTimeMillis()
    val grain: Future[GrainRef] = runtime.createGrain[GreeterGrain]()
    val grainRef = Await.result(grain, 5 seconds)

    println(s"That took ${System.currentTimeMillis() - time}ms")
    println(grainRef)
  }
}
