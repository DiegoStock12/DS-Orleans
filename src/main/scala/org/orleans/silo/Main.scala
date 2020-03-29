package org.orleans.silo

import ch.qos.logback.classic.Level
import org.orleans.developer.twitter.{Twitter, TwitterAccount}
import org.orleans.silo.Services.Grain.GrainRef
import org.orleans.silo.Test.GreeterGrain
import org.orleans.silo.storage.GrainDatabase
import org.orleans.silo.utils.ServerConfig

import scala.concurrent.ExecutionContext
import collection.JavaConverters._

object Main {

  def main(args: Array[String]): Unit = {
    setLevel(Level.INFO) // The debug level might give a little bit too much info.

    GrainDatabase.setApplicationName("HelloWorld")

    /**
      * A simple test-scenario is run here.
      */
    val master = Master()
      .registerGrain[Twitter]
      .registerGrain[TwitterAccount]
      .setHost("localhost")
      .setTCPPort(1400)
      .setUDPPort(1500)
      .setExecutionContext(ExecutionContext.global)
      .setGrainPorts((1501 to 1510).toSet)
      .build()

    val slave = Slave()
      .registerGrain[Twitter]
      .registerGrain[TwitterAccount]
      .setHost("localhost")
      .setTCPPort(1600)
      .setUDPPort(1700)
      .setMasterHost("localhost")
      .setMasterTCPPort(1400)
      .setMasterUDPPort(1500)
      .setExecutionContext(ExecutionContext.global)
      .setGrainPorts((1601 to 1610).toSet)
      .build()

    val slave2 = Slave()
      .registerGrain[Twitter]
      .registerGrain[TwitterAccount]
      .setHost("localhost")
      .setTCPPort(1800)
      .setUDPPort(1900)
      .setMasterHost("localhost")
      .setMasterTCPPort(1400)
      .setMasterUDPPort(1500)
      .setExecutionContext(ExecutionContext.global)
      .setGrainPorts((1801 to 1810).toSet)
      .build()

    master.start()
    slave.start()
    slave2.start()

    Thread.sleep(1000 * 20)

    //master.stop()
    //slave.stop()
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
