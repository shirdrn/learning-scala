package org.shirdrn.scala.akka.cluster

import akka.actor.{Props, Actor, ActorLogging, ActorSystem}
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory
import org.shirdrn.scala.akka.Start

class AkkaCluster extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  def receive = {
    case Start => log.info("Akka cluster working: " + Start)
    case msg => log.warning("Unknown nessage: " + msg)
  }
}

object AkkaClusterApplication extends App {

  val system = ActorSystem("cluster-system", ConfigFactory.load().getConfig("OurCluster"))
  system.actorOf(Props[AkkaCluster], name = "clusterActor")
}
