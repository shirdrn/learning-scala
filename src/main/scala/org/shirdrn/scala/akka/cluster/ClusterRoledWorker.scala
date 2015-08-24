package org.shirdrn.scala.akka.cluster

import akka.actor._
import akka.cluster.{Member, Cluster}
import akka.cluster.ClusterEvent.{UnreachableMember, MemberEvent, InitialStateAsEvents}

abstract class ClusterRoledWorker extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  var workers = IndexedSeq.empty[ActorRef]

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def register(member: Member, f: (Member) => ActorPath): Unit = {
    val actorPath = f(member)
    log.info("Actor path: " + actorPath)
    val actorSelection = context.actorSelection(actorPath)
    actorSelection ! Registration
  }
}
