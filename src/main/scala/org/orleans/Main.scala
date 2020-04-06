package org.orleans
import ch.qos.logback.classic.Level
import org.orleans.client.OrleansRuntime
import org.orleans.developer.twitter.{
  Twitter,
  TwitterAccount,
  TwitterAcountRef,
  TwitterRef
}

import scala.concurrent.ExecutionContext.Implicits.global
import org.orleans.silo.Main.setLevel
import org.orleans.silo.storage.{GrainDatabase, MongoGrainDatabase}
import org.orleans.silo.{Master, Slave}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object Main {

  def main(args: Array[String]): Unit = {
    setLevel(Level.INFO) // The debug level might give a little bit too much info.
    args.toList match {
      case level :: "master" :: tail if isInt(level) => {
        if (level.toInt == 1)
          setLevel(Level.DEBUG)
        processMasterArgs(tail)
      }
      case level :: "slave" :: tail if isInt(level) => {
        if (level.toInt == 1)
          setLevel(Level.DEBUG)
        processSlaveArgs(tail)
      }
      case level :: "client" :: tail if isInt(level) => {
        if (level.toInt == 1)
          setLevel(Level.DEBUG)
        processClientArgs(tail)
      }
      case _ =>
        new RuntimeException(
          "Unknown command, please specify if you want to start a master, client or slave."
        )
    }
  }

  //MASTERHOST TCPPORT UDPPORT GRAINPORTSTART GRAINPORTEND
  //Example: 1400 1500 1501 1510
  def processMasterArgs(args: List[String]) = args match {
    case tcpPort :: udpPort :: grainPortStart :: graintPortEnd :: Nil
        if isInt(tcpPort) &&
          isInt(udpPort) &&
          isInt(grainPortStart) &&
          isInt(graintPortEnd) =>
      runMaster(
        sys.env("HOSTNAME") + ".orleans",
        tcpPort.toInt,
        udpPort.toInt,
        grainPortStart.toInt,
        graintPortEnd.toInt
      )
    case _ =>
      throw new RuntimeException(
        "Unknown command, run as: 'master TCPPORT UDPPORT GRAINPORTSTART GRAINPORTEND'"
      )
  }

  //TCPPORT UDPPORT GRAINPORTSTART GRAINPORTEND MASTERHOST MASTERTCP MASTERUDP
  //Example: localhost 1400 1500 1501 1510 master-host 1400 1500
  def processSlaveArgs(args: List[String]) = args match {
    case tcpPort :: udpPort :: grainPortStart :: graintPortEnd :: masterHost :: masterTCP :: masterUDP :: Nil
        if isInt(tcpPort) &&
          isInt(udpPort) &&
          isInt(grainPortStart) &&
          isInt(graintPortEnd) &&
          isInt(masterTCP) &&
          isInt(masterUDP) =>
      runSlave(
        sys.env("HOSTNAME") + ".slave-headless.orleans",
        tcpPort.toInt,
        udpPort.toInt,
        grainPortStart.toInt,
        graintPortEnd.toInt,
        masterHost,
        masterTCP.toInt,
        masterUDP.toInt
      )
    case _ =>
      throw new RuntimeException(
        "Unknown command, run as: 'slave TCPPORT UDPPORT GRAINPORTSTART GRAINPORTEND MASTERHOST MASTERTCP MASTERUDP'"
      )
  }

  def processClientArgs(args: List[String]) = args match {
    case scenarioId :: masterHost :: masterTCP :: Nil
        if isInt(scenarioId) && isInt(masterTCP) =>
      runClientScenario(scenarioId.toInt, masterHost, masterTCP.toInt)
    case _ =>
      throw new RuntimeException(
        "Unknown command, run as: 'client SCENARIOID MASTERHOST MASTERTCP"
      )
  }

  def runClientScenario(id: Int, masterHost: String, masterTCP: Int) = {
    println(s"Now trying to run scenario $id.")
    id match {
      case 1 => runClientScenarioOne(masterHost, masterTCP)
      case 2 => runClientScenarioTwo(masterHost, masterTCP)
      case 3 => runClientScenarioThree(masterHost, masterTCP)
      case _ => throw new RuntimeException("Can't find client scenario.")
    }
    println(
      s"Running scenario $id finished."
    )
  }

  def runMaster(host: String,
                tcp: Int,
                udp: Int,
                grainPortStart: Int,
                graintPortEnd: Int): Unit = {
    GrainDatabase.disableDatabase = true
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
  }

  def runSlave(host: String,
               tcp: Int,
               udp: Int,
               grainPortStart: Int,
               graintPortEnd: Int,
               masterHost: String,
               masterTCP: Int,
               masterUDP: Int): Unit = {
    GrainDatabase.disableDatabase = true
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
  }

  def runClientScenarioOne(masterHost: String, tcpPort: Int) = {
    GrainDatabase.disableDatabase = true
    println(GrainDatabase.instance)
    val runtime = OrleansRuntime()
      .registerGrain[Twitter]
      .registerGrain[TwitterAccount]
      .setHost(masterHost)
      .setPort(tcpPort)
      .build()

    var time = System.currentTimeMillis()
    val twitterFuture: Future[TwitterRef] =
      runtime.createGrain[Twitter, TwitterRef]()
    val twitter = Await.result(twitterFuture, 5 seconds)
    println(
      s"Creating a Twitter grain took ${System.currentTimeMillis() - time}ms")
  }

  def runClientScenarioTwo(masterHost: String, tcpPort: Int) = {
    GrainDatabase.disableDatabase = true
    val runtime = OrleansRuntime()
      .registerGrain[Twitter]
      .registerGrain[TwitterAccount]
      .setHost(masterHost)
      .setPort(tcpPort)
      .build()

    var time = System.currentTimeMillis()
    val twitterFuture: Future[TwitterRef] =
      runtime.createGrain[Twitter, TwitterRef]()
    val twitter = Await.result(twitterFuture, 5 seconds)
    println(
      s"Creating a Twitter grain took ${System.currentTimeMillis() - time}ms")

    val users = 10
    println(s"Now creating $users users.")
    time = System.currentTimeMillis()
    for (i <- (1 to users)) {
      val user: TwitterAcountRef =
        Await.result(twitter.createAccount(s"wouter-${i}"), 5 seconds)
    }

    //Await.result(twitter.createAccount(s"wouter-${i}"), 1 seconds)
    println(s"That took ${System.currentTimeMillis() - time}ms")

    println(s"Now searching those $users users and show following list.")
    time = System.currentTimeMillis()
    for (i <- (1 to users)) {
      val user = Await.result(twitter.getAccount(s"wouter-${i}"), 50 seconds)
      for (j <- (1 to users)) {
        if (i != j) {
          Await.result(user.followUser(twitter, s"wouter-${j}"), 5 seconds)
        }
      }

      val followers = Await.result(user.getFollowingList(), 50 seconds)
      println(s"wouter-${i} - ${followers.size} followers")
    }

    //Await.result(twitter.createAccount(s"wouter-${i}"), 1 seconds)
    println(s"That took ${System.currentTimeMillis() - time}ms")

    println(s"Now searching those $users users and send 10 000 tweets.")
    time = System.currentTimeMillis()
    for (i <- (1 to users)) {
      val user = Await.result(twitter.getAccount(s"wouter-${i}"), 50 seconds)
      var futures: List[Future[Any]] = List()
      for (j <- (1 to 10000)) {
        user.tweet("I like dis")
      }

    }
    //Await.result(twitter.createAccount(s"wouter-${i}"), 1 seconds)
    println(s"That took ${System.currentTimeMillis() - time}ms")

    println("Waiting 5 seconds so all tweets are processed.")
    Thread.sleep(5000)
    println(s"Now searching those $users users and get amount of tweets..")
    time = System.currentTimeMillis()
    for (i <- (1 to users)) {
      val user = Await.result(twitter.getAccount(s"wouter-${i}"), 50 seconds)
      val size = Await.result(user.getAmountOfTweets(), 50 seconds)
      println(s"wouter-${i} - $size tweets")
    }
    println(s"That took ${System.currentTimeMillis() - time}ms")

    println(s"Now searching those $users users and get all tweets.")
    time = System.currentTimeMillis()
    for (i <- (1 to users)) {
      val user = Await.result(twitter.getAccount(s"wouter-${i}"), 50 seconds)
      val tweets = Await.result(user.getTweets(), 50 seconds)
      println(s"wouter-${i} - ${tweets.size} tweets")
    }
    println(s"That took ${System.currentTimeMillis() - time}ms")

    println(s"Now searching those $users users and get their timeline")
    time = System.currentTimeMillis()
    for (i <- (1 to users)) {
      val user = Await.result(twitter.getAccount(s"wouter-${i}"), 50 seconds)
      val tweets = Await.result(user.getTimeline(twitter), 50 seconds)
      println(s"wouter-${i} - ${tweets.size} tweets")
    }
    println(s"That took ${System.currentTimeMillis() - time}ms")
  }

  def runClientScenarioThree(masterHost: String, tcpPort: Int): Unit = {
    GrainDatabase.disableDatabase = true
    val runtime = OrleansRuntime()
      .registerGrain[Twitter]
      .registerGrain[TwitterAccount]
      .setHost(masterHost)
      .setPort(tcpPort)
      .build()

    var time = System.currentTimeMillis()
    val twitterFuture: Future[TwitterRef] =
      runtime.createGrain[Twitter, TwitterRef]()
    val twitter = Await.result(twitterFuture, 5 seconds)
    println(
      s"Creating a Twitter grain took ${System.currentTimeMillis() - time}ms")

    val users = 100000
    println(s"Now creating $users users.")
    time = System.currentTimeMillis()
    for (i <- (1 to users)) {
      val user: TwitterAcountRef =
        Await.result(twitter.createAccount(s"test-${i}"), 5 seconds)
    }

    var run = true
    Future
      .sequence((1 to users).toList.map(i => twitter.createAccount(s"test-$i")))
      .onComplete {
        case Success(list) => {
          println(s"That took ${System.currentTimeMillis() - time}ms")
          run = false
        }
        case Failure(exception) => {
          println(exception)
          run = false
        }
      }

    while (run) {
      Thread.sleep(50)
    }
    //Await.result(twitter.createAccount(s"wouter-${i}"), 1 seconds)
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
