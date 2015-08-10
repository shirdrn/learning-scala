package org.shirdrn.scala

import akka.actor.{ActorLogging, Actor, ActorSystem, Props}
import akka.event.LoggingAdapter
import com.typesafe.config.ConfigFactory
import net.sf.json.JSONObject

import scala.language.postfixOps

class LocalServer extends Actor with ActorLogging {

  def receive = {
    case Start => println("start")
    case Stop => println("stop")
    case Heartbeat(id, magic) => println("Heartbeat" + (id, magic))
    case Header(id, len, encrypted) => println("Header" + (id, len, encrypted))
    case Packet(id, seq, content) => println("Packet" + (id, seq, content))
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
