package org.orleans.silo

import ch.qos.logback.classic.Level
import org.orleans.silo.Services.Grain.GrainRef
import org.orleans.silo.Test.GreeterGrain
import org.orleans.silo.utils.ServerConfig

import scala.concurrent.ExecutionContext

object Main {

  def main(args: Array[String]): Unit = {
    setLevel(Level.INFO) // The debug level might give a little bit too much info.

    /**
      * A simple test-scenario is run here.
      */
    val master = Master()
      .registerGrain[GreeterGrain]
      .setHost("localhost")
      .setTCPPort(1400)
      .setUDPPort(1500)
      .setExecutionContext(ExecutionContext.global)
      .setGrainPorts((1501 to 1510).toSet)
      .build()

    val slave = Slave()
      .registerGrain[GreeterGrain]
      .setHost("localhost")
      .setTCPPort(1600)
      .setUDPPort(1700)
      .setMasterHost("localhost")
      .setMasterTCPPort(1400)
      .setMasterUDPPort(1500)
      .setExecutionContext(ExecutionContext.global)
      .setGrainPorts((1601 to 1610).toSet)
      .build()

    master.start()
    slave.start()



    // Let main thread sleep for 5 seconds
    Thread.sleep(1000 * 5)

    // Let see if other slaves are aware of each other.
    println(slave.getSlaves())
    println(master.getSlaves())

//    master.stop()
//    slave.stop()
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
