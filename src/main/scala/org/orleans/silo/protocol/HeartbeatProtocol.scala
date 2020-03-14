package org.orleans.silo.protocol

object HeartbeatProtocol {

  // The interval for which
  val heartbeatInterval: Long = 1000
  val deathTime: Long = 5 * heartbeatInterval

  case class HeartbeatPacket(uuid: String, timestamp: Long)

  case class HandshakePacket(uuid: String,
                             ip_address: String,
                             port: Int,
                             timestamp: String)

  case class WelcomePacket(uuid: String)

  case class ShutdownPacket(uuid: String)
}
