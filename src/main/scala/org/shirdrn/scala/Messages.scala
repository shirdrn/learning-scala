package org.shirdrn.scala

object MyStart extends Serializable
object MyStop extends Serializable

trait MyMessage extends Serializable {
  val id: String
}

case class MyHeartbeat(id: String, magic:Int) extends MyMessage
case class MyHeader(id: String, len: Int, encrypted: Boolean) extends MyMessage
case class MyPacket(id: String, seq: Long, content: String) extends MyMessage
