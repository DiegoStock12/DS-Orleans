package org.orleans.silo

import scala.concurrent.ExecutionContext

object Main {
  def main(args: Array[String]): Unit = {

    if (args(0) == "master") {
      Master.start()

    } else if (args(0) == "slave") {
      val master_ip = args(1)
    }
  }
}
