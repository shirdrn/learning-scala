package org.shirdrn.scala

import akka.actor._
import akka.event.LoggingAdapter
import com.typesafe.config.ConfigFactory
import net.sf.json.JSONObject

import scala.language.postfixOps

class ClientActor extends Actor with ActorLogging {

  // akka.<protocol>://<actor system>@<hostname>:<port>/<actor path>
  val path = "akka.tcp://remote-system@127.0.0.1:2552/user/remoteActor"
  val remoteServerRef = context.actorSelection(path)

  def receive = {
    case Start => {
      log.info("Send start command to server...")
      remoteServerRef ! Start
    }
    case Stop => {
      log.info("Send stop command to server...")
      remoteServerRef ! Stop
    }
    case hb: Heartbeat => {
      log.info("Send heartbeat to server...")
      remoteServerRef ! hb
    }
    case header: Header => {
      log.info("Send header to server...")
      remoteServerRef ! header
    }
    case pkt: Packet => {
      log.info("Send packet to server...")
      remoteServerRef ! pkt
    }
    case cmd: Shutdown => {
      log.info("Send shutdown command to server...")
      remoteServerRef ! cmd
    }
    case m => log.info("Unknown message: " + m)
  }

}

object ScalaClientApplication extends App {
  val system = ActorSystem("client-system", ConfigFactory.load().getConfig("MyRemoteClientSideActor"))
  val clientActor = system.actorOf(Props[ClientActor], "clientActor")
  @volatile var running = true

  val hbWorker = new Thread("HB-WORKER") {
    override def run(): Unit = {
      while(running) {
        clientActor ! Heartbeat("HB", 39264)
        Thread.sleep(3000)
      }
    }
  }
  hbWorker.start

  clientActor ! Start
  Thread.sleep(2000)

  clientActor ! Header("HEADER", 20, encrypted=false)
  Thread.sleep(2000)

  val pkt = new JSONObject()
  pkt.put("txid", 90760001)
  pkt.put("pvid", "CMCC")
  pkt.put("txtm", "2015-08-10 14:01:25")
  pkt.put("payp", "2015-08-10 14:01:25")
  pkt.put("amount", 100)
  clientActor ! Packet("PKT", System.currentTimeMillis, pkt.toString)
  Thread.sleep(2000)

  Thread.sleep(2000)
  running = false

  clientActor ! Shutdown("SHUTDOWN", 5000)
  system.shutdown
}
