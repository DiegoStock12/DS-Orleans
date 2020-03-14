package main.scala.org.orleans.silo

import ch.qos.logback.classic.Level
import org.orleans.silo.communication.ConnectionProtocol.MasterConfig

object Main {

  def main(args: Array[String]): Unit = {
    setLevel(Level.INFO)
    val master = new Master("localhost", 123)
    val slave = new Slave("localhost", 124, MasterConfig("localhost", 123))
    slave.start()

    //start master after 2 seconds
    Thread.sleep(2000)
    master.start()

    // Let main thread sleep for 15 seconds
    Thread.sleep(1000 * 15)

    slave.stop()
    master.stop()
  }

  def setLevel(level: Level) = {
    val logger: ch.qos.logback.classic.Logger =
      org.slf4j.LoggerFactory
        .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
        .asInstanceOf[(ch.qos.logback.classic.Logger)]
    logger.setLevel(level)
  }

}
