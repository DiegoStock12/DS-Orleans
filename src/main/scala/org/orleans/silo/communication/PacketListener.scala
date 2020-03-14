package org.orleans.silo.communication
import org.orleans.silo.communication.ConnectionProtocol.Packet

trait PacketListener {
  def onReceive(packet: Packet, host: String, port: Int)
}
