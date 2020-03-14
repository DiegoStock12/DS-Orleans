package main.scala.org.orleans.silo

object Main {

  def main(args: Array[String]): Unit = {
    val master = new Master("localhost", 123)
    master.start()
    // Sleep for 10 seconds
    Thread.sleep(10 * 1000)

    master.stop()
  }

}
