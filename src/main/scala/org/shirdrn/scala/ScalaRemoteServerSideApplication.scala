package org.shirdrn.scala

import akka.actor.{ActorLogging, Actor, ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.kernel.Bootable
import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

class RemoteActor extends Actor with ActorLogging {
  def receive = {
    case Start => {
      println("Receive event: " + Start)
    }
    case Stop => {
      println("Receive event: " + Stop)
    }
    case Shutdown(_, waitSecs) => {
      println("Wait to shutdown: waitSecs=" + waitSecs)
      Thread.sleep(waitSecs)
      println("Shutdown this system.")
      context.system.shutdown
    }
    case Heartbeat(id, magic) => println("Receive heartbeat: " + (id, magic))
    case Header(id, len, encrypted) => println("Receive header: " + (id, len, encrypted))
    case Packet(id, seq, content) => println("Receive packet: " + (id, seq, content))
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
  println("Remote server actor started: " + system)

  system.actorOf(Props[RemoteActor], "remoteActor")

}
