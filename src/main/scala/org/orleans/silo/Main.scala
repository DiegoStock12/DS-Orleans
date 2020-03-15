package org.orleans.silo

import main.scala.org.orleans.silo.Slave

import scala.concurrent.ExecutionContext

object Main {
  def main(args: Array[String]): Unit = {

    if (true) {
      Master.start()
      Slave.start()
      println("Started master and slave")

    } else if (false) {
      val master_ip = args(1)
      Slave.start()
    }
  }
}
