package org.orleans.silo

import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}
import org.orleans.silo.hello.{GreeterGrpc, HelloRequest, HelloReply}

import scala.concurrent.{ExecutionContext, Future}

object HelloWorldServer {
  private val logger = Logger.getLogger(classOf[HelloWorldServer].getName)

  def main(args: Array[String]): Unit = {
    val server = new HelloWorldServer(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50050

}

class HelloWorldServer(executionContext: ExecutionContext) {
  self =>
  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder.forPort(HelloWorldServer.port).addService(GreeterGrpc.bindService(new GreeterImpl, executionContext)).build.start
    HelloWorldServer.logger.info("Server started, listening on " + HelloWorldServer.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }
}

private class GreeterImpl extends GreeterGrpc.Greeter{
  override def sayHello(request: HelloRequest): Future[HelloReply] = {
    val reply = HelloReply(message = "Hello "+request.name)
    Future.successful(reply)
  }
}
