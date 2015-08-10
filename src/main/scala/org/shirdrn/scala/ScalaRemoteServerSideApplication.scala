package org.shirdrn.scala

import akka.actor.{ActorLogging, Actor, ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.kernel.Bootable
import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

class RemoteActor extends Actor with ActorLogging {
  def receive = {
    case MyStart => {
      println("Receive event: " + MyStart)
    }
    case MyStop => {
      println("Receive event: " + MyStop)
    }
    case MyHeartbeat(id, magic) => println("Receive heartbeat: " + (id, magic))
    case MyHeader(id, len, encrypted) => println("Receive header: " + (id, len, encrypted))
    case MyPacket(id, seq, content) => println("Receive packet: " + (id, seq, content))
    case _ =>
  }
}


object ScalaServerSideApplication extends Bootable {

  // Remote actor
  // http://agiledon.github.io/blog/2014/02/18/remote-actor-in-akka/
  val system = ActorSystem("remote-system", ConfigFactory.load().getConfig("MyRemoteServerSideActor"))
  println("Remote server side actor started: " + system)

  def startup = {
    system.actorOf(Props[RemoteActor], "remoteActor")
  }

  def shutdown = {
    system.shutdown
  }

}

object ScalaServerApplication extends App {

  val system = ActorSystem("remote-system", ConfigFactory.load().getConfig("MyRemoteServerSideActor"))
  println("Remote server side actor started: " + system)

  system.actorOf(Props[RemoteActor], "remoteActor")

}
