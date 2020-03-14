package org.orleans.silo.communication

import java.net.{
  DatagramPacket,
  DatagramSocket,
  InetSocketAddress,
  SocketException
}
import java.nio.charset.StandardCharsets
import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.communication.ConnectionProtocol.{Packet, PacketType}
import org.orleans.silo.communication.{ConnectionProtocol => protocol}

class PacketManager(listener: PacketListener, port: Int)
    extends Runnable
    with LazyLogging {

  val socket: DatagramSocket = new DatagramSocket(port)

  @volatile
  private var running: Boolean = true

  def init(id: String = UUID.randomUUID().toString.split("-")(0)) = {
    val packetThread = new Thread(this)
    packetThread.setName(s"packetmgr-$id")
    packetThread.start()
  }

  def run(): Unit = {
    while (running) {
      val bytes = new Array[Byte](protocol.maxBuffer)
      val packet: DatagramPacket = new DatagramPacket(bytes, bytes.length)

      try {
        socket.receive(packet)
      } catch {
        case e: SocketException => {
          if (running) { // The socket was closed, while we didn't stop the manager. Otherwise we expect this exception.
            logger.error(
              "Socket was closed while PacketManager was still running.")
          }
          return
        }
      }

      val parsedPacket = protocol
        .toPacket(packet.getData)
        .getOrElse({
          logger.error(
            s"Received a packet from ${packet.getAddress.getHostName}/${packet.getPort} but couldn't parse it.")
          return
        })

      listener.onReceive(parsedPacket,
                         packet.getAddress.getHostAddress,
                         packet.getPort)
      Thread.sleep(100)
    }
  }

  def send(packet: Packet, host: String, port: Int) = {
    val packetBytes: Array[Byte] =
      protocol.fromPacket(packet).getBytes(StandardCharsets.UTF_8)

    socket.send(
      new DatagramPacket(packetBytes,
                         packetBytes.length,
                         new InetSocketAddress(host, port)))
  }

  def cancel(): Unit = {
    this.running = false
    socket.close()
  }

}
