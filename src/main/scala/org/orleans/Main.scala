package org.orleans
import ch.qos.logback.classic.Level
import org.orleans.client.OrleansRuntime
import org.orleans.developer.twitter.{Twitter, TwitterAccount}
import org.orleans.silo.Main.setLevel
import org.orleans.silo.{Master, Slave}

import scala.concurrent.ExecutionContext

object Main {

  def main(args: Array[String]): Unit = {
    setLevel(Level.INFO) // The debug level might give a little bit too much info.
    args.toList match {
      case "master" :: tail => processMasterArgs(tail)
      case "slave" :: tail  => processSlaveArgs(tail)
      case "client" :: tail => processClientArgs(tail)
    }
  }

  //MASTERHOST TCPPORT UDPPORT GRAINPORTSTART GRAINPORTEND
  //Example: localhost 1400 1500 1501 1510
  def processMasterArgs(args: List[String]) = args match {
    case masterHost :: tcpPort :: udpPort :: grainPortStart :: graintPortEnd :: Nil
        if isInt(tcpPort) &&
          isInt(udpPort) &&
          isInt(grainPortStart) &&
          isInt(graintPortEnd) =>
      runMaster(masterHost,
                tcpPort.toInt,
                udpPort.toInt,
                grainPortStart.toInt,
                graintPortEnd.toInt)
    case _ =>
      throw new RuntimeException(
        "Unknown command, run as: 'master MASTERHOST TCPPORT UDPPORT GRAINPORTSTART GRAINPORTEND'")
  }

  //SLAVEHOST TCPPORT UDPPORT GRAINPORTSTART GRAINPORTEND MASTERHOST MASTERTCP MASTERUDP
  //Example: localhost 1400 1500 1501 1510 master-host 1400 1500
  def processSlaveArgs(args: List[String]) = args match {
    case slaveHost :: tcpPort :: udpPort :: grainPortStart :: graintPortEnd :: masterHost :: masterTCP :: masterUDP :: Nil
        if isInt(tcpPort) &&
          isInt(udpPort) &&
          isInt(grainPortStart) &&
          isInt(graintPortEnd) &&
          isInt(masterTCP) &&
          isInt(masterUDP) =>
      runSlave(slaveHost,
               tcpPort.toInt,
               udpPort.toInt,
               grainPortStart.toInt,
               graintPortEnd.toInt,
               masterHost,
               masterTCP.toInt,
               masterUDP.toInt)
    case _ =>
      throw new RuntimeException(
        "Unknown command, run as: 'slave SLAVEHOST TCPPORT UDPPORT GRAINPORTSTART GRAINPORTEND MASTERHOST MASTERTCP MASTERUDP'")
  }

  def processClientArgs(args: List[String]) = args match {
    case scenarioId :: masterHost :: masterTCP :: Nil
        if isInt(scenarioId) && isInt(masterTCP) =>
      runClientScenario(scenarioId.toInt, masterHost, masterTCP.toInt)
    case _ =>
      throw new RuntimeException(
        "Unknown command, run as: 'client SCENARIOID MASTERHOST MASTERTCP")
  }

  def runClientScenario(id: Int, masterHost: String, masterTCP: Int) = {
    println(s"Now trying to run scenario $id.")
    id match {
      case 1 => runClientScenarioOne(masterHost, masterTCP)
      case _ => throw new RuntimeException("Can't find client scenario.")
    }
    println(
      s"Running scenario $id finished. Now running until explicitly stopped.")

    while (true) {
      Thread.sleep(1000)
    }
  }

  def runMaster(host: String,
                tcp: Int,
                udp: Int,
                grainPortStart: Int,
                graintPortEnd: Int): Unit =
    Master()
      .registerGrain[Twitter]
      .registerGrain[TwitterAccount]
      .setHost(host)
      .setTCPPort(tcp)
      .setUDPPort(udp)
      .setExecutionContext(ExecutionContext.global)
      .setGrainPorts((grainPortStart to graintPortEnd).toSet)
      .build()
      .start()

  def runSlave(host: String,
               tcp: Int,
               udp: Int,
               grainPortStart: Int,
               graintPortEnd: Int,
               masterHost: String,
               masterTCP: Int,
               masterUDP: Int): Unit =
    Slave()
      .registerGrain[Twitter]
      .registerGrain[TwitterAccount]
      .setHost(host)
      .setTCPPort(tcp)
      .setUDPPort(udp)
      .setExecutionContext(ExecutionContext.global)
      .setGrainPorts((grainPortStart to graintPortEnd).toSet)
      .setMasterHost(masterHost)
      .setMasterTCPPort(masterTCP.toInt)
      .setMasterUDPPort(masterUDP.toInt)
      .build()
      .start()

  def runClientScenarioOne(masterHost: String, tcpPort: Int) = {
    val runtime = OrleansRuntime()
      .registerGrain[Twitter]
      .registerGrain[TwitterAccount]
      .setHost(masterHost)
      .setPort(tcpPort)
      .build()

  }

  /** Very hacky way to set the log level */
  def setLevel(level: Level) = {
    val logger: ch.qos.logback.classic.Logger =
      org.slf4j.LoggerFactory
        .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
        .asInstanceOf[(ch.qos.logback.classic.Logger)]
    logger.setLevel(level)
  }

  def isInt(x: String) = x forall Character.isDigit
}
