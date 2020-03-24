package org.orleans.silo.Test

import java.io.{ObjectInputStream, ObjectOutputStream}

import org.orleans.silo.Services.Grain.GrainRef
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.Success


object Testing {
  // Just a test for the new Service client
  def main(args: Array[String]): Unit = {
    println("Trying to get the socket")
    // Socket for the dispatcher
    // Get the grain reference
    val g = GrainRef("1234", "localhost", 2500)

    // Send a message to the grain
    // Synchronous request
    println("Sending hello message to grain 1234")
    g ! "hello"

    // TODO right now either one or the other calls work, because the connection in the dispatcher closes
    // after addressing the first request, we need some kind of task queue that points to the place to reply to
    // Maybe add a queue to the dispatcher from where the threads execute!
    Thread.sleep(5000)


    // Asynchronous request
    g ? 2 onComplete ({
      case Success(value) => println(value)
      case _ => println("not working")
    })

    Thread.sleep(5000)


    //    val f = g ? 2
    //    f onComplete({
    //      case Success(value) => println(value)
    //      case _ => println("not working")
    //    })
    //
    //    println(f)
    //    Thread.sleep(4000)


    //          val dispatcherSocket: Socket = new Socket("localhost", 2500)
    //          val oos: ObjectOutputStream = new ObjectOutputStream(dispatcherSocket.getOutputStream)
    //          val ois: ObjectInputStream = new ObjectInputStream(dispatcherSocket.getInputStream)
    //          val request = HelloRequest("Diego")
    //        time {
    //          oos.writeObject(request)
    //          println(s"sent $request to dispatcher")
    //          val o = ois.readObject()
    //          println(o)
    //        }

    //Thread.sleep(15000)

  }

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) / 1e6 + "ms")
    result
  }
}
