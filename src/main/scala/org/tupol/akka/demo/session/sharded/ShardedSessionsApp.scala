package org.tupol.akka.demo.session.sharded

import akka.actor.AddressFromURIString
import akka.actor.typed.ActorSystem
import com.typesafe.config.{ Config, ConfigFactory, ConfigRenderOptions }
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._

// PORT=12552 \
// SID=`curl -s  http://127.0.0.1:$PORT/session/create | jq -r '.value' `; \
// curl -s http://127.0.0.1:$PORT/session/$SID/find ; \
// echo; \
// echo; echo "curl -s http://127.0.0.1:$PORT/session/create" ; \
// echo; \
// echo; echo "curl -s http://127.0.0.1:$PORT/session/$SID/find" ; \
// echo; \
// curl -X POST -H "Content-Type: application/json" -d '{"key": "a", "value": "b"}' -k http://127.0.0.1:$PORT/session/$SID/update

object ShardedSessionsApp {
  val log = LoggerFactory.getLogger(this.getClass)

  val config = ConfigFactory.load("application-cluster.conf")

  def configWithPort(port: Int): Config =
    ConfigFactory.parseString(s"""
       akka.remote.artery.canonical.port = $port
        """).withFallback(config)

  def main(args: Array[String]): Unit = {
    val seedNodePorts =
      config.getStringList("akka.cluster.seed-nodes").asScala.flatMap { case AddressFromURIString(s) => s.port }
    val ports = args.headOption match {
      case Some(port) => Seq(port.toInt)
      case None       => seedNodePorts ++ Seq(0)
    }

    ports.foreach { port =>
      println(s"### Starting service on port: $port")
      val httpPort =
        if (port > 0) 10000 + port // offset from akka port
        else 0                     // let OS decide

      val config = configWithPort(port)
      ActorSystem[Nothing](ServerGuardian(httpPort), "sessions-app", config)
    }
  }

  def main2(args: Array[String]): Unit = {
    val config     = ConfigFactory.load("application-cluster.conf")
    val properties = args.filter(_.startsWith("-D")).map(_.replaceFirst("-D", "")).mkString("\n")
    println(s"""Arguments
               |$properties""".stripMargin)
    val appConfig = ConfigFactory.parseString(properties).withFallback(config)
    val httpPort  = appConfig.getInt("akka.http.server.default-http-port")

    ActorSystem[Nothing](ServerGuardian(httpPort), "sessions-app", appConfig)
  }

}
