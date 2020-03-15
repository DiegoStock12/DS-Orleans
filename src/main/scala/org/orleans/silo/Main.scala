package main.scala.org.orleans.silo

import ch.qos.logback.classic.Level
import org.orleans.silo.communication.ConnectionProtocol.MasterConfig

object Main {

  def main(args: Array[String]): Unit = {
    setLevel(Level.DEBUG) // The debug level might give a little bit too much info.

    /**
      * This is just a scenario with 3 slaves and 1 master.
      * The master is started shortly after the slaves so that the slaves do some handshake attempts first.
      * Then, they end up sending each other heartbeats.
      * Finally after 10 seconds, both master and slaves are shut down.
      */
    val master = new Master("localhost", 123)
    val slave = new Slave("localhost", 124, MasterConfig("localhost", 123))
    val slave2 = new Slave("localhost", 125, MasterConfig("localhost", 123))
    val slave3 = new Slave("localhost", 126, MasterConfig("localhost", 123))
    //slave.start()
    //slave2.start()
    //slave3.start()

    //start master after 2 seconds
    //Thread.sleep(2000)
    master.start()
    slave.start()
    slave2.start()
    slave3.start()

    // Let main thread sleep for 10 seconds
    Thread.sleep(1000 * 5)
    slave.stop()
    Thread.sleep(1000 * 15)

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
