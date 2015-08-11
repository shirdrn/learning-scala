package org.shirdrn.scala.akka

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import net.sf.json.JSONObject

import scala.language.postfixOps

class LocalServer extends Actor with ActorLogging {

  def receive = {
    case Start => log.info("start")
    case Stop => log.info("stop")
    case Heartbeat(id, magic) => log.info("Heartbeat" + (id, magic))
    case Header(id, len, encrypted) => log.info("Header" + (id, len, encrypted))
    case Packet(id, seq, content) => log.info("Packet" + (id, seq, content))
    case _ =>
  }
}


object ScalaLocalActor extends App {
  // Local actor
  val localServer = ActorSystem("local-server")
  println(localServer)
  val localActorRef = localServer.actorOf(Props(new LocalServer()), name="local-server")
  println(localActorRef)
  localActorRef ! Start
  localActorRef ! Heartbeat("3099100", 0xabcd)

  val content = new JSONObject()
  content.put("name", "Stone")
  content.put("empid", 51082001)
  content.put("score", 89.36581)
  localActorRef ! Packet("3000001", System.currentTimeMillis(), content.toString)
  localActorRef ! Stop
  localServer shutdown

}
