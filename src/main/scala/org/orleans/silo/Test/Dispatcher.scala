package org.orleans.silo.Test

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.{ServerSocket, Socket}
import java.util.concurrent.{ConcurrentHashMap, Executors, ThreadPoolExecutor}

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain

/**
 * Dispatcher that will hold the messages for a certain type of grain
 *
 * @param grain grain to start the dispatcher with
 * @tparam T type of the grain that the dispatcher will serve
 */
class Dispatcher[T <: Grain](grain: T, private val port: Int)
    extends Runnable
    with LazyLogging {
  type GrainType = T

  // Thread pool to execute new request
  private val pool: ThreadPoolExecutor = Executors.newFixedThreadPool(10).asInstanceOf[ThreadPoolExecutor]
  val grainMap: ConcurrentHashMap[String, Grain] = new ConcurrentHashMap[String, Grain]()
  // Example grain
  grainMap.put("1234", grain)
  // Receive requests through here
  val socket: ServerSocket = new ServerSocket(port)
  logger.info(s"Dispatcher for ${grain.getClass} started in port $port")


  override def run(): Unit = {

    while(true){
      // Wait for requests
      val s : Socket = socket.accept()
      logger.info(s"Accepted request from ${s.getInetAddress}${s.getPort}")
      val ois : ObjectInputStream = new ObjectInputStream(s.getInputStream)
      val oos = new ObjectOutputStream(s.getOutputStream)
      ois.readObject() match {
        case req  =>
          val g: T = grainMap.get("1234").asInstanceOf[T]
          pool.execute(() => {
            g.receive(req.asInstanceOf[g.Request])
//            oos.writeObject(resp)
          })
      }

    }
  }
}
