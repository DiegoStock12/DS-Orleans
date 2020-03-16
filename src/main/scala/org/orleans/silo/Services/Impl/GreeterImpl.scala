package org.orleans.silo.Services.Impl

import org.orleans.silo.hello.{GreeterGrpc, HelloReply, HelloRequest}

import scala.concurrent.Future

class GreeterImpl extends GreeterGrpc.Greeter {

  println("Here greeter service waiting on port 50400 on the slave")

  override def sayHello(request: HelloRequest): Future[HelloReply] = {
    println("Received a request by " + request.name)
    val reply = HelloReply(message = "Hello " + request.name)
    Future.successful(reply)
  }
}
