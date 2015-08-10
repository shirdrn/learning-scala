package org.shirdrn.scala

import akka.actor.{ActorLogging, Actor, ActorSystem, Props}
import akka.event.LoggingAdapter
import com.typesafe.config.ConfigFactory
import net.sf.json.JSONObject

import scala.language.postfixOps

class ClientActor extends Actor with ActorLogging {

  // akka.<protocol>://<actor system>@<hostname>:<port>/<actor path>
  val path = "akka.tcp://remote-system@127.0.0.1:2552/user/remoteActor"
  val remoteServerRef = context.actorSelection(path)

  def receive = {
    case MyStart => {
      println("Send to server: MyStart")
      remoteServerRef ! MyStart
    }
    case MyStop => {
      println("Send to server: MyStop")
      remoteServerRef ! MyStop
    }
    case hb: MyHeartbeat => {
      println("Send heartbeat to server...")
      remoteServerRef ! hb
    }
    case pkt: MyPacket => {
      println("Send packet to server...")
      remoteServerRef ! pkt
    }
    case m => println("Unknown message: " + m)
  }

}

object ScalaClientApplication extends App {
  val system = ActorSystem("client-system", ConfigFactory.load().getConfig("MyRemoteClientSideActor"))
  val clientActor = system.actorOf(Props[ClientActor], "clientActor")
  @volatile var running = true

  val hbWorker = new Thread("HB-WORKER") {
    override def run(): Unit = {
      while(running) {
        clientActor ! MyHeartbeat("HB", 39264)
        Thread.sleep(3000)
      }
    }
  }
  hbWorker.start

  clientActor ! MyStart
  Thread.sleep(2000)

  clientActor ! MyHeader("HEADER", 20, encrypted=false)
  Thread.sleep(2000)

  val pkt = new JSONObject()
  pkt.put("txid", 90760001)
  pkt.put("pvid", "CMCC")
  pkt.put("txtm", "2015-08-10 14:01:25")
  pkt.put("payp", "2015-08-10 14:01:25")
  pkt.put("amount", 100)
  clientActor ! MyPacket("PKT", System.currentTimeMillis, pkt.toString)
  Thread.sleep(2000)

  Thread.sleep(10000)
  running = false
  system.shutdown
}
