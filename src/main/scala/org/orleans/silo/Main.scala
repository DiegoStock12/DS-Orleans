package org.orleans.silo

import scala.concurrent.ExecutionContext

object Main {
  def main(args: Array[String]): Unit = {

    if (true) {
      Master.start()

    } else if (false) {
      val master_ip = args(1)
      Slave.start()
    }
  }
}
