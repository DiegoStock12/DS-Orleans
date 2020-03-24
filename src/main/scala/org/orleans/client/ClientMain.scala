package org.orleans.client
import main.scala.org.orleans.client.OrleansRuntime
import org.orleans.silo.Services.Grain.GrainRef
import org.orleans.silo.Test.GreeterGrain

import scala.concurrent.Future

object ClientMain {

  def main(args: Array[String]): Unit = {
    val runtime = OrleansRuntime()
      .registerGrain[GreeterGrain]
      .setHost("localhost")
      .setPort(1400)
      .build()

    val grain: Future[GrainRef] = runtime.createGrain[GreeterGrain]()
  }
}
