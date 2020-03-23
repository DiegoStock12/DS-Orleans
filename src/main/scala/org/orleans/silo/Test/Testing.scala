package org.orleans.silo.Test

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.Socket

import org.orleans.silo.hello.HelloRequest

object Testing {
  // Just a test for the new Service client
  def main(args: Array[String]): Unit = {
    // Socket for the dispatcher

          val dispatcherSocket: Socket = new Socket("localhost", 2500)
          val oos: ObjectOutputStream = new ObjectOutputStream(dispatcherSocket.getOutputStream)
          val ois: ObjectInputStream = new ObjectInputStream(dispatcherSocket.getInputStream)
          val request = HelloRequest("Diego")
        time {
          oos.writeObject(request)
          println(s"sent $request to dispatcher")
          val o = ois.readObject()
          println(o)
        }

//    val client = ServiceFactory.createGrainService("localhost", 50050)
//    val f = client.createGrain[GreeterGrpc.Greeter, GreeterGrain]()
//    Await.result(f, 10 seconds)
//    f onComplete {
//      case Success(res) => println(s"Success response $res")
//      case Failure(e) => e.printStackTrace()
//    }



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
