package org.shirdrn.scala.akka.cluster

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import org.shirdrn.scala.akka._

import scala.language.postfixOps

class ClusterServerActor extends Actor with ActorLogging {

  def receive = {
    case Start => {
      log.info("RECV event: " + Start)
    }
    case Stop => {
      log.info("RECV event: " + Stop)
    }
    case Packet(id, seq, content) => log.info("RECV packet: " + (id, seq, content))
    case _ =>
  }
}

object AkkaClusterServerApplication extends App {

  val systems = Map("a" -> "MyServerActor", "b" -> "YourServerActor", "c" -> "HisServerActor")
  systems.keys.foreach { name =>
    val path = systems(name)
    val system = ActorSystem("cluster-system", ConfigFactory.load().getConfig(path))
    system.actorOf(Props[AkkaCluster], "clusterActor")
    println("Server actor started: name=" + name + ", path=" + path + ", system=" + system)
  }

}
