package org.orleans.silo.communication
import java.nio.charset.StandardCharsets

object ConnectionProtocol {

  object PacketType {
    val HEARTBEAT = "heartbeat"
    val HANDSHAKE = "handshake"
    val WELCOME = "welcome"
    val SHUTDOWN = "shutdown"
  }

  case class MasterConfig(host: String, udpPort: Int = 161)
  case class MasterInfo(uuid: String, lastHeartbeat: Long, config: MasterConfig)
  case class SlaveInfo(uuid: String,
                       host: String,
                       port: Int,
                       lastHeartbeat: Long)

  // The interval for which
  val heartbeatInterval: Long = 1000
  val deathTime: Long = 5 * heartbeatInterval

  val packetSeparator: Char = ','

  val maxBuffer: Int = 1024

  case class Packet(packetType: String, uuid: String, timestamp: Long) {}

  // Handshake protocol
  val connectionAttempts: Int = 5
  val connectionDelay: Long = 1000

  def toPacket(packet: String): Option[Packet] =
    packet.split(packetSeparator).toList match {
      case ty :: uuid :: timestamp :: Nil
          if parseLong(timestamp.trim()) != None =>
        Some(Packet(ty.trim(), uuid.trim(), parseLong(timestamp.trim()).get))
      case _ => None // Couldn't parse this packet
    }

  def toPacket(packet: Array[Byte]): Option[Packet] =
    toPacket(new String(packet, StandardCharsets.UTF_8))

  def fromPacket(packet: Packet): String =
    packet.packetType + packetSeparator + packet.uuid + packetSeparator + packet.timestamp

  def parseLong(s: String) = try { Some(s.toLong) } catch { case _ => None }
}
