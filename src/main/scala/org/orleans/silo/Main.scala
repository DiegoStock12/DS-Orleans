package org.orleans.silo

import ch.qos.logback.classic.Level
import org.orleans.silo.createGrain.CreateGrainGrpc
import org.orleans.silo.createGrain.CreateGrainGrpc.CreateGrain
import org.orleans.silo.twitterAcount.TwitterGrpc
import org.orleans.silo.utils.ServerConfig
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

object Main {

  def main(args: Array[String]): Unit = {
    setLevel(Level.INFO) // The debug level might give a little bit too much info.

    TwitterGrpc.javaDescriptor.getMethods.asScala.foreach(x =>
      println(x.getFullName))

    /**
      * A simple test-scenario is run here.
      */
    val master =
      new Master(ServerConfig("localhost", 2000, 50050),
                 ExecutionContext.global)
    val slave = new Slave(slaveConfig = ServerConfig("localhost", 2001, 50060),
                          masterConfig = ServerConfig("localhost", 2000, 50050),
                          ExecutionContext.global)
    //slave.start()
    //slave2.start()
    //slave3.start()

    master.start()
    slave.start()
    // Stop one slave.
    //slave.stop()
    //Thread.sleep(1000 * 15)

    // See if the awareness is updated.
    //println(master.getSlaves())

    // Stop all by stopping the master.
    //master.stop()
  }

  /** Very hacky way to set the log level */
  def setLevel(level: Level) = {
    val logger: ch.qos.logback.classic.Logger =
      org.slf4j.LoggerFactory
        .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
        .asInstanceOf[(ch.qos.logback.classic.Logger)]
    logger.setLevel(level)
  }

}
