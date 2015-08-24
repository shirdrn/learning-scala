package org.shirdrn.scala.akka.cluster

import akka.actor._
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, MemberUp, UnreachableMember}
import akka.cluster.{Cluster, Member}

abstract class ClusterRoledWorker extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  var workers = IndexedSeq.empty[ActorRef]

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberUp], classOf[UnreachableMember], classOf[MemberEvent])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def register(member: Member, f: (Member) => ActorPath): Unit = {
    val actorPath = f(member)
    log.info("Actor path: " + actorPath)
    val actorSelection = context.actorSelection(actorPath)
    actorSelection ! Registration
  }
}
