package org.orleans.silo.communication
import java.nio.charset.StandardCharsets

/**
  * Stores all (meta-)data for the communication protocol.
  */
object ConnectionProtocol {

  // A set of packet types.
  object PacketType {
    val HEARTBEAT = "heartbeat"
    val HANDSHAKE = "handshake"
    val WELCOME = "welcome"
    val SHUTDOWN = "shutdown"
  }

  // Data we keep track of in our master and slaves.
  case class MasterConfig(host: String, udpPort: Int)
  case class MasterInfo(uuid: String, lastHeartbeat: Long, config: MasterConfig)
  case class SlaveInfo(uuid: String,
                       host: String,
                       port: Int,
                       lastHeartbeat: Long)

  // The interval for which heart beats are sent.
  val heartbeatInterval: Long = 1000

  // Time before we consider a silo dead.
  val deathTime: Long = 5 * heartbeatInterval

  // Handshake connection attempts
  val connectionAttempts: Int = 5

  // Handshake connection delay (i.e. after how many ms do we retry the handshake)
  val connectionDelay: Long = 1000

  // Separator for packet fields.
  val packetSeparator: Char = ','

  // Simple structure to keep packet data.
  case class Packet(packetType: String, uuid: String, timestamp: Long)

  /**
    * Parses packet from String to Packet.
    * @param packet the packet to parse.
    * @return Option[Packet] otherwise None if it can't be parsed.
    */
  def toPacket(packet: String): Option[Packet] =
    packet.split(packetSeparator).toList match {
      case ty :: uuid :: timestamp :: Nil
          if parseLong(timestamp.trim()) != None =>
        Some(Packet(ty.trim(), uuid.trim(), parseLong(timestamp.trim()).get))
      case _ => None // Couldn't parse this packet
    }

  /**
    * Parses packet from byte array to Packet.
    * @param packet the packet to parse.
    * @return Option[Packet] otherwise None if it can't be parsed.
    */
  def toPacket(packet: Array[Byte]): Option[Packet] =
    toPacket(new String(packet, StandardCharsets.UTF_8))

  /**
    * Transforms packet into a String using the packetSeparator.
    * @param packet the packet to stringify.
    * @return a stringified packet.
    */
  def fromPacket(packet: Packet): String =
    packet.packetType + packetSeparator + packet.uuid + packetSeparator + packet.timestamp

  /** Helper function to parse a Long. **/
  def parseLong(s: String) = try { Some(s.toLong) } catch { case _ => None }
}
