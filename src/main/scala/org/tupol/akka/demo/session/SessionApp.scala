package org.tupol.akka.demo.session

import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory

import scala.util.{ Failure, Success }

object SessionApp extends App {
  val config = ConfigFactory.load("application-simple.conf")
  ActorSystem[Nothing](ServerGuardian(18899), "sessions-app", config)
}

object ServerGuardian {

  def apply(httpPort: Int): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    val sessionsGuardian: ActorRef[SessionsGuardian.Request] =
      context.spawn(SessionsGuardian.applyWithInternalChildren(), "sessions-guardian")
    val routes = new SessionRoutes(sessionsGuardian, context.system).routes
    Server.start(routes, httpPort, context.system)
    Behaviors.empty
  }

}

object Server {
  import akka.actor.typed.ActorSystem
  import akka.actor.CoordinatedShutdown
  import akka.{ Done, actor => classic }
  import akka.http.scaladsl.Http
  import akka.http.scaladsl.server.Route
  import scala.concurrent.duration._

  /**
   * Logic to bind the given routes to a HTTP port and add some logging around it
   */
  def start(routes: Route, port: Int, system: ActorSystem[_]): Unit = {
    import akka.actor.typed.scaladsl.adapter._
    import system.executionContext
    implicit val classicSystem: classic.ActorSystem = system.toClassic
    val shutdown                                    = CoordinatedShutdown(classicSystem)

    // Akka http API changes, a conversation for another time
    // Http().newServerAt("localhost", port).bind(routes)

    Http().bindAndHandle(routes, "localhost", port).onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(s"Server online at http://${address.getHostString}:${address.getPort}/")

        shutdown.addTask(
          CoordinatedShutdown.PhaseServiceRequestsDone,
          "http-graceful-terminate"
        ) { () =>
          binding.terminate(10.seconds).map { _ =>
            system.log.info(s"Server http://${address.getHostString}:${address.getPort}/ graceful shutdown completed.")
            Done
          }
        }
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
}

// curl -X POST -H "Content-Type: application/json" -d "{\"key\": \"a\", \"value\": \"b\"}" -k http://127.0.0.1:12552/session/639e4eb2-ae1b-4654-a63d-900f5f0e7799/update
