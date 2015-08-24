package org.shirdrn.scala.akka.cluster

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster.Member
import akka.cluster.protobuf.msg.ClusterMessages.MemberStatus
import com.typesafe.config.ConfigFactory
import net.sf.json.JSONObject
import org.shirdrn.scala.akka.cluster.utils.DatetimeUtils

class EventIntercepter extends ClusterRoledWorker {

  @volatile var InterceptedRecords : Int = 0
  val IP_PATTERN = "[^\\s]+\\s+\\[([^\\]]+)\\].+\"(\\d+\\.\\d+\\.\\d+\\.\\d+)\"".r
  val blackIpList = Array(
    "5.9.116.101", "103.42.176.138", "123.182.148.65", "5.45.64.205",
    "27.159.226.192", "76.164.228.218", "77.79.178.186", "104.200.31.117",
    "104.200.31.32", "104.200.31.238", "123.182.129.108", "220.161.98.39",
    "59.58.152.90", "117.26.221.236", "59.58.150.110", "123.180.229.156",
    "59.60.123.239", "117.26.222.6", "117.26.220.88", "59.60.124.227",
    "142.54.161.50", "59.58.148.52", "59.58.150.85", "202.105.90.142"
  ).toSet

  log.info("Black IP count: " + blackIpList.size)
  blackIpList.foreach(log.info(_))

  def receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
      register(member, getCollectorPath)
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach(register(_, getCollectorPath))
    case UnreachableMember(member) =>
      log.info("Member detected as Unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
    case _: MemberEvent => // ignore

    case Registration => {
      context watch sender
      workers = workers :+ sender
      log.info("Processor registered: " + sender)
      log.info("Registered processors: " + workers.size)
    }
    case Terminated(processingActorRef) =>
      workers = workers.filterNot(_ == processingActorRef)
    case NginxRecord(sourceHost, eventCode, line) => {
      val (isIpInBlackList, data) = checkRecord(eventCode, line)
      if(!isIpInBlackList) {
        InterceptedRecords += 1
        if(workers.size > 0) {
          val processorIndex = (if (InterceptedRecords < 0) 0 else InterceptedRecords) % workers.size
          workers(processorIndex) ! FilteredRecord(sourceHost, eventCode, line, data.getString("eventdate"), data.getString("realip"))
          log.info("Details: processorIndex=" + processorIndex + ", processors=" + workers.size)
        }
        log.info("Intercepted data: data=" + data)
      } else {
        log.info("Discarded: " + line)
      }
    }
  }

  def getCollectorPath(member: Member): ActorPath = {
    RootActorPath(member.address) / "user" / "collectingActor"
  }

  private def checkRecord(eventCode: String, line: String): (Boolean, JSONObject) = {
    val data: JSONObject = new JSONObject()
    var isIpInBlackList = false
    IP_PATTERN.findFirstMatchIn(line).foreach { m =>
      val rawDt = m.group(1)
      val dt = DatetimeUtils.format(rawDt)
      val realIp = m.group(2)

      data.put("eventdate", dt)
      data.put("realip", realIp)
      data.put("eventcode", eventCode)
      isIpInBlackList = blackIpList.contains(realIp)
    }
    (isIpInBlackList, data)
  }
}

object EventIntercepter extends App {

  Seq("2851","2852").foreach { port =>
    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
      .withFallback(ConfigFactory.parseString("akka.cluster.roles = [intercepter]"))
      .withFallback(ConfigFactory.load())
    val system = ActorSystem("event-cluster-system", config)
    val processingActor = system.actorOf(Props[EventIntercepter], name = "interceptingActor")
    system.log.info("Processing Actor: " + processingActor)
  }
}
