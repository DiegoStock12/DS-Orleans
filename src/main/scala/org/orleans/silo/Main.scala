package org.orleans.silo

object Main {

  def main(args: Array[String]): Unit = {

    if (args(0) == "master") {
      println("Hey master")



    } else if (args(0) == "slave") {
      val master_ip = args(1)
    }
  }

}
