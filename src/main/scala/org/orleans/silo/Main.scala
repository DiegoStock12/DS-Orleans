package main.scala.org.orleans.silo

import ch.qos.logback.classic.Level
import org.orleans.silo.communication.ConnectionProtocol.MasterConfig

import scala.concurrent.ExecutionContext

object Main {

  def main(args: Array[String]): Unit = {
    setLevel(Level.DEBUG) // The debug level might give a little bit too much info.

    /**
      * A simple test-scenario is run here.
      */
    val master = new Master("localhost", 123, ExecutionContext.global)
    val slave = new Slave("localhost",
                          124,
                          MasterConfig("localhost", 123),
                          ExecutionContext.global)
    //slave.start()
    //slave2.start()
    //slave3.start()

    master.start()
    slave.start()

    // Let main thread sleep for 5 seconds
    Thread.sleep(1000 * 5)

    // Let see if other slaves are aware of each other.
    println(slave.getSlaves())
    println(master.getSlaves())

    // Stop one slave.
    slave.stop()
    Thread.sleep(1000 * 15)

    // See if the awareness is updated.
    println(master.getSlaves())

    // Stop all by stopping the master.
    master.stop()
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
