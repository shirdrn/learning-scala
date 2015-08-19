package org.shirdrn.scala.akka

import java.io.ByteArrayOutputStream
import java.net.{HttpURLConnection, URL}
import java.sql.{Connection, DriverManager, SQLException}
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue}

import akka.actor._

import scala.concurrent.forkjoin.ForkJoinPool
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

case class WebUrl(link: String)
case class ScheduledWebUrl(link: String, config: Map[String, Any])
case class CrawledWeb(link: String, domain: String, encoding: String, contentLength: Int, outlinks: Set[String])
case class Stored(link: String, outlinkCount: Int)

object MySQLUtils {

  val driverClass = "com.mysql.jdbc.Driver"
  val jdbcUrl = "jdbc:mysql://10.10.4.130:3306/page_db"
  val user = "web"
  val password = "web"

  try {
    Class.forName(driverClass)
  } catch {
    case e: ClassNotFoundException => throw e
    case e: Exception => throw e
  }

  @throws(classOf[SQLException])
  def getConnection: Connection = {
    DriverManager.getConnection(jdbcUrl, user, password)
  }

  @throws(classOf[SQLException])
  def doTrancation(transactions: Set[String]) : Unit = {
    val connection = getConnection
    connection.setAutoCommit(false)
    transactions.foreach {
      connection.createStatement.execute(_)
    }
    connection.commit
  }
}

object DatetimeUtils {

  val DEFAULT_DT_FORMAT = "yyyy-MM-dd HH:mm:ss"

  def format(timestamp: Long, format: String): String = {
    val df = new SimpleDateFormat(format)
    df.format(new Date(timestamp))
  }

  def format(timestamp: Long): String = {
    val df = new SimpleDateFormat(DEFAULT_DT_FORMAT)
    df.format(new Date(timestamp))
  }

}

class CrawlActor extends Actor with ActorLogging {

  private val scheduleActor = context.actorOf(Props[ScheduleActor], "schedule_actor")
  private val storeActor = context.actorOf(Props[PageStoreActor], "store_actor")
  private val q = new LinkedBlockingQueue[String]()
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool())

  def receive = {
    case link: String => {
      if(link != null && link.startsWith("http://")) {
        log.info("Checked: " + link)
        scheduleActor ! WebUrl(link)
      }
    }
    case ScheduledWebUrl(link, _) => {
      var crawledWeb: CrawledWeb = null
      val crawlFuture = Future {
        try {
          var encoding = "utf-8"
          var outlinks: Set[String] = Set[String]()
          val u = new URL(link)
          val domain = u.getHost
          val uc = u.openConnection().asInstanceOf[HttpURLConnection]
          uc.setConnectTimeout(5000)
          uc.connect()
          if (uc.getResponseCode == 200) {
            // page encoding
            if (uc.getContentEncoding != null) {
              encoding = uc.getContentEncoding
            }
            // page content
            if (uc.getContentLength > 0) {
              val in = uc.getInputStream
              val buffer = Array.fill[Byte](512)(0)
              val baos = new ByteArrayOutputStream
              var bytesRead = in.read(buffer)
              while (bytesRead > -1) {
                baos.write(buffer, 0, bytesRead)
                bytesRead = in.read(buffer)
              }
              outlinks = extractOutlinks(link, baos.toString(encoding))
              baos.close
            }
            log.info("Page: link=" + link + ", encoding=" + encoding + ", outlinks=" + outlinks)
            CrawledWeb(link, domain, encoding, uc.getContentLength, outlinks)
          }
        } catch {
          case e: Throwable => {
            log.error("Crawl error: " + e.toString)
            e
          }
        }
      }
      crawlFuture.onSuccess {
        case crawledWeb: CrawledWeb => {
          log.info("Succeed to crawl: link=" + link + ", crawledWeb=" + crawledWeb)
          if(crawledWeb != null) {
            storeActor ! crawledWeb
            log.info("Sent crawled data to store actor.")
            q add link
          }
        }
      }
      crawlFuture.onFailure {
        case exception: Throwable => log.error("Fail to crawl: " + exception.toString)
      }
    }
    case Stored(link, count) => {
      q.remove(link)
      scheduleActor ! (link, count)
    }
  }

  def extractOutlinks(parentUrl: String, content: String): Set[String] = {
    val outlinks = "href\\s*=\\s*\"([^\"]+)\"".r.findAllMatchIn(content).map { m =>
      var url = m.group(1)
      if(!url.startsWith("http")) {
        url = new URL(new URL(parentUrl), url).toExternalForm
      }
      url
    }.toSet
    outlinks.filter( url => !url.isEmpty && (url.endsWith("html") || url.endsWith("htm")))
  }
}

class PageStoreActor extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool())
  var crawlerRef = context.actorOf(Props[CrawlActor], name="crawl-actor")

  def receive = {
    case CrawledWeb(link, domain, encoding, contentLength, outlinks) => {
      val future = Future {
        var sqls = Set[String]()
        try {
          val createTime = DatetimeUtils.format(System.currentTimeMillis)
          val sql = "INSERT INTO web_link VALUES ('" + link + "','" + domain + "','" + encoding + "'," + contentLength + ",'" + createTime + "')"
          log.info("Link SQL: " + sql)
          sqls += sql
          var outlinksSql = "INSERT INTO web_outlink VALUES "
          outlinksSql += outlinks.map("('" + link + "','" + _ + "','" + createTime + "')").mkString(",")
          log.info("Outlinks SQL: " + outlinksSql)
          sqls += outlinksSql
          MySQLUtils.doTrancation(sqls)
          (link, outlinks.size)
        } catch {
          case e: Throwable => throw e
        }
      }
      future.onSuccess {
        case (link: String, outlinkCount: Int) => {
          log.info("SUCCESS: link=" + link + ", outlinkCount=" + outlinkCount)
          crawlerRef ! Stored(link, outlinkCount)
        }
      }
      future.onFailure {
        case e: Throwable  => throw e
      }
    }
  }
}

class ScheduleActor() extends Actor with ActorLogging {

  val config = Map(
    "domain.black.list" -> Seq("google.com", "facebook.com", "twitter.com"),
    "crawl.retry.times" -> 3,
    "filter.page.url.suffixes" -> Seq(".zip", ".avi", ".mkv")
  )
  val counter = new ConcurrentHashMap[String, Int]()

  def receive = {
    case WebUrl(url) => {
      sender ! ScheduledWebUrl(url, config)
    }
    case (link: String, count: Int) => {
      counter.put(link, count)
      log.info("Counter: " + counter.toString)
    }
  }

}

object ScheduleActor {

  def sendFeeds(crawlerActorRef: ActorRef, seeds: Seq[String]): Unit = {
    seeds.foreach(crawlerActorRef ! _)
  }
}

object AkkaFutureCallback {

  def main(args: Array[String]) {
    val system = ActorSystem("crawler-system")
    system.log.info(system.toString)

    val scheduleActorRef = system.actorOf(Props[ScheduleActor], name="schedule-actor")
    val storeActorRef = system.actorOf(Props[PageStoreActor], name="store-actor")

    val crawlActorRef = system.actorOf(Props[CrawlActor], name="crawl-actor")

    val links =
      """
        |http://apache.org
        |http://csdn.net
        |http://hadoop.apache.org
        |http://spark.apache.org
        |http://nutch.apache.org
        |http://storm.apache.org
        |http://mahout.apache.org
        |http://flink.apache.org
        |http://fdajlkdjfakfjlkadjflkajflakjf.com
      """.stripMargin
    val seeds: Seq[String] = links.split("\\s+").toSeq
    ScheduleActor.sendFeeds(crawlActorRef, seeds)

  }

}
