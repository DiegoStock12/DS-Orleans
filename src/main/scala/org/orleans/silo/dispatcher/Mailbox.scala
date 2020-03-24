package org.orleans.silo.dispatcher

import java.io.ObjectOutputStream
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue

import com.typesafe.scalalogging.LazyLogging
import org.orleans.silo.Services.Grain.Grain

/**
 * Message that will be saved
 * @param id id of the grain to be delivered to
 * @param msg message to be delivered, should be any of the messages a grain expects
 * @param sender stream the grain can respond to
 */
case class Message(id: String, msg: Any, sender: Sender)


/**
 * Sender companion object
 */
object Sender{
  def apply(oos: ObjectOutputStream): Sender = new Sender(oos)
}
/**
 * How we're gonna pass the sender to the grain so he can reply to the message
 * @param stream ObjectOutputStream that the sender can use to write back to the source
 */
class Sender(private val stream: ObjectOutputStream){
  def !(msg: Any) = stream.writeObject(msg)
}

/**
 * Mailbox to be used by the Grains to receive messages
 * These are executable so they can be run and the messages can be received
 * @param grain Grain that the message queue makes reference to
 */
private[dispatcher] class Mailbox (val grain: Grain) extends Runnable with LazyLogging{
  private[dispatcher] val inbox = new LinkedBlockingQueue[Message]
  // id of the mailbox
  val id: String = grain._id

  // length of the message queue for that actor
  @volatile
  var length : Int = inbox.size()

  /**
   * Adds a new message to the inbox
   * @param msg
   * @return
   */
  def addMessage(msg : Message) = {
    logger.info(s"Appending new message to queue $msg")
    this.inbox.add(msg)
  }

  @volatile
  var isRunning : Boolean = false

  /**
   * To check if the mailbox is empty
   * @return whether the mailbox has messages
   */
  @volatile
  def isEmpty = this.inbox.isEmpty

  /**
   * Run this mailbox, which delivers messages to
   * grains so they are processed
   */
  override def run(): Unit = {
    // Run until inbox is empty
    // TODO maybe this could be preempted so there's no
    // starvation if a grain has a lot of messages
    this.isRunning = true
    while(inbox.peek() != null){
      val msg : Message = inbox.poll()
      grain.receive((msg.msg, msg.sender))
    }
    this.isRunning = false
  }

  /**
   * We need to override equals to use it as hashmap key
   * @param obj
   * @return
   */
  override def equals(obj: Any): Boolean = {
    obj match {
      case mbox : Mailbox => {
        mbox.isInstanceOf[Mailbox] &&
        mbox.id == this.id
      }
      case _ => false
    }
  }

  /**
   * So we can use the mailbox as key of the hashmap,
   * the hashcode just hashes the mailbox returning the
   * @return
   */
  override def hashCode(): Int = {
    this.id.hashCode
  }


}
