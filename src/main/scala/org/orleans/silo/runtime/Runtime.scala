package org.orleans.silo.runtime

import java.{lang, util}
import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._
import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain
import org.orleans.silo.utils.ServerConfig

import scala.util.Random

object Runtime{
  // Class that will serve as index for the grain map
  // TODO state should be taken from an enum
  case class GrainInfo(id: String, state:String, grain: Grain)
}

/**
 * Server runtime
 * - Keep the information of the grains and their information
 * - Keep track of the state of the grains periodically to deactivate/ replicate
 * - Manage the communications with other servers' runtime
 *
 */
class Runtime(val config: ServerConfig)
    extends Runnable
    with LazyLogging{

  import Runtime._

  // Hashmap that will save the current grain information, indexed by port
  val grainMap : ConcurrentHashMap[Int, GrainInfo] = new ConcurrentHashMap[Int, GrainInfo]()

  // Free ports that the Server will manage to give to Grains
  // We keep a concurrent hashset so the elements can be added and removed safely
  val freePorts: ConcurrentHashMap.KeySetView[Int, lang.Boolean] = ConcurrentHashMap.newKeySet()
  private val ports = (5000 to 20000).toList
  // Have to do this to add multiple at one (a bit hacky but the only way)
  freePorts.addAll(new util.ArrayList[Int](ports.asJava))

  /**
   * Get the head of the ports and return it
   * @return an unused port
   */
  def getFreePort: Int = {
    val port : Int = freePorts.toArray().head.asInstanceOf[Int]
    logger.info(s"removing port $port")
    freePorts.remove(port)
    port
  }


  /**
   * This should check the grains and check the load of them, in order to
   * keep the load balanced.
   * - If a grain is idle for a long time, store it
   * - If a grain is too loaded, initiate the replication
   */
  override def run(): Unit = {
    logger.info("Runtime running")
  }


}
