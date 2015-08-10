package org.shirdrn.scala

object Start extends Serializable
object Stop extends Serializable

trait Message extends Serializable {
  val id: String
}

case class Shutdown(id: String, waitSecs: Int) extends Message
case class Heartbeat(id: String, magic:Int) extends Message
case class Header(id: String, len: Int, encrypted: Boolean) extends Message
case class Packet(id: String, seq: Long, content: String) extends Message
