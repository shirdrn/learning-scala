package org.shirdrn.scala.akka

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.kernel.Bootable
import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

class RemoteActor extends Actor with ActorLogging {

  def receive = {
    case Start => {
      log.info("RECV event: " + Start)
    }
    case Stop => {
      log.info("RECV event: " + Stop)
    }
    case Shutdown(waitSecs) => {
      log.info("Wait to shutdown: waitSecs=" + waitSecs)
      Thread.sleep(waitSecs)
      log.info("Shutdown this system.")
      context.system.shutdown
    }
    case Heartbeat(id, magic) => log.info("RECV heartbeat: " + (id, magic))
    case Header(id, len, encrypted) => log.info("RECV header: " + (id, len, encrypted))
    case Packet(id, seq, content) => log.info("RECV packet: " + (id, seq, content))
    case _ =>
  }
}


object AkkaServerBootableApplication extends Bootable {

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

object AkkaServerApplication extends App {

  val system = ActorSystem("remote-system", ConfigFactory.load().getConfig("MyRemoteServerSideActor"))
  println("Remote server actor started: " + system)

  system.actorOf(Props[RemoteActor], "remoteActor")

}
