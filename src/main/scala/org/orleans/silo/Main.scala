package org.orleans.silo

import ch.qos.logback.classic.Level
import org.orleans.silo.utils.ServerConfig

import scala.concurrent.ExecutionContext

object Main {

  def main(args: Array[String]): Unit = {
    setLevel(Level.INFO) // The debug level might give a little bit too much info.

    /**
      * A simple test-scenario is run here.
      */
    val master =
      new Master(ServerConfig("localhost", 1500, 50050), ExecutionContext.global)
    val slave = new Slave(slaveConfig =  ServerConfig("localhost", 1600, 50060),
                          masterConfig = ServerConfig("localhost", 1500, 50050),
                          ExecutionContext.global, report = true)
//    val slave2 = new Slave(slaveConfig =  ServerConfig("localhost", 1601, 50061),
//                            masterConfig = ServerConfig("localhost", 1500, 50050),
//                            ExecutionContext.global, report = false)
    //slave.start()
    //slave2.start()
    //slave3.start()

    master.start()
    slave.start()
    //slave2.start()

    // Let main thread sleep for 5 seconds
    Thread.sleep(1000 * 5)

    // Let see if other slaves are aware of each other.
    println(slave.getSlaves())
    println(master.getSlaves())

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
