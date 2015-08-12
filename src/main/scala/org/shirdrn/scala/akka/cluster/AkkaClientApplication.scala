//package org.shirdrn.scala.akka.cluster
//
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.concurrent.atomic.AtomicLong
//
//import akka.actor._
//import com.typesafe.config.ConfigFactory
//import net.sf.json.JSONObject
//import org.shirdrn.scala.akka.{Packet, Start, Stop, Shutdown}
//
//import scala.language.postfixOps
//import scala.util.Random
//
//class ClusterClientActor extends Actor with ActorLogging {
//
//  // akka.<protocol>://<actor system>@<hostname>:<port>/<actor path>
//
//  val clusterActorRefs = {
//    val ports = Seq(2551, 2552, 2553)
//    val refs = collection.mutable.Set[ActorRef]()
//    ports.foreach { port =>
//      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).withFallback(ConfigFactory.load())
//      val system = ActorSystem("ClusterSystem", config)
//      refs += system.actorOf(Props[ClusterClientActor], name = "clusterListener")
//    }
//    refs.toSeq
//  }
//
//  val r = Random
//
//  def getActorRef: ActorRef = {
//    clusterActorRefs(r.nextInt(clusterActorRefs.size))
//  }
//
//  def receive = {
//    case pkt: Packet => send(getActorRef, pkt)
//    case m => log.info("Unknown message: " + m)
//  }
//
//  private def send(actorRef: ActorRef, cmd: Serializable): Unit = {
//    log.info("Send command to server: " + cmd)
//    try {
//      actorRef ! cmd
//    } catch {
//      case e: Exception => {
//        log.info("Try to connect by sending Start command...")
//      }
//    }
//  }
//
//}
//
//class ClusterManagerActor extends Actor with ActorLogging {
//
//  // akka.<protocol>://<actor system>@<hostname>:<port>/<actor path>
//
//  val clusterActorSelection = {
//    val path = "akka.tcp://cluster-system@127.0.0.1:61816/user/clusterActor"
//    context.actorSelection(path)
//  }
//
//  def receive = {
//    case Start => {
//      send(clusterActorSelection, Start)
//    }
//    case Stop => {
//      send(clusterActorSelection, Stop)
//    }
//  }
//
//  private def send(actorSelection: ActorSelection, cmd: Serializable): Unit = {
//    log.info("Send command to server: " + cmd)
//    try {
//      actorSelection ! cmd
//    } catch {
//      case e: Exception => {
//        log.info("Try to connect by sending Start command...")
//      }
//    }
//  }
//
//}
//
//class ClientApplication {
//
//  val DEFAULT_PACKET_COUNT = 10
//  val ID = new AtomicLong(90760000)
//
//  val system = ActorSystem("client-system", ConfigFactory.load().getConfig("ClusterClientActor"))
//  val clusterClientActor = system.actorOf(Props[ClusterClientActor], "clusterClientActor")
//
//  // send some packets
//  val DT_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"
//  val r = Random
//  val packetCount = 10
//  val serviceProviders = Seq("CMCC", "AKBBC", "OLE")
//  val payServiceProvicers = Seq("PayPal", "CMB", "ICBC", "ZMB", "XXB")
//
//  def nextTxID: Long = {
//    ID.incrementAndGet()
//  }
//
//  def format(timestamp: Long, format: String): String = {
//    val df = new SimpleDateFormat(format)
//    df.format(new Date(timestamp))
//  }
//
//  def createPacket(packet: Map[String, _]): JSONObject = {
//    val pkt = new JSONObject()
//    packet.foreach(p => pkt.put(p._1, p._2))
//    pkt
//  }
//
//  def nextProvider(seq: Seq[String]): String = {
//    seq(r.nextInt(seq.size))
//  }
//
//  clusterClientActor ! Start
//  Thread.sleep(2000)
//
//  def sendMessages(nPackets: Int): Unit = {
//    var packetCount = DEFAULT_PACKET_COUNT
//    if(nPackets > 0) {
//      packetCount = nPackets
//    }
//    // start to send messages
//    val startWhen = System.currentTimeMillis()
//    for(i <- 0 until packetCount) {
//      val pkt = createPacket(Map[String, Any](
//        "txid" -> nextTxID,
//        "pvid" -> nextProvider(serviceProviders),
//        "txtm" -> format(System.currentTimeMillis(), DT_FORMAT),
//        "payp" -> nextProvider(payServiceProvicers),
//        "amount" -> 1000 * r.nextFloat()))
//      clusterClientActor ! Packet("PKT", System.currentTimeMillis, pkt.toString)
//      Thread.sleep(3000)
//    }
//    val finishWhen = System.currentTimeMillis()
//    println("FINISH: timeTaken=" + (finishWhen - startWhen) + ", avg=" + packetCount/(finishWhen - startWhen))
//  }
//
//  def shutdown: Unit = {
//    clusterClientActor ! Stop
//    Thread.sleep(3000)
//    system.shutdown
//  }
//}
//
//object AkkaClientApplication extends App {
//
//  // client send messages to remote server actor
//  val client = new ClientApplication
//  client.sendMessages(10)
//  client.shutdown
//
//  // cluster manager sends messages to cluster actor
//  val system = ActorSystem("client-system", ConfigFactory.load().getConfig("ClusterClientActor"))
//  val clusterClientActor = system.actorOf(Props[ClusterManagerActor], "clusterClientActor")
//  for(i <- 0 until 10) {
//    clusterClientActor ! Start
//  }
//}
