package org.tupol.akka.demo.session.sharded

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import org.tupol.akka.demo.session.{ Server, SessionsGuardian }

object ServerGuardian {

  def apply(httpPort: Int): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    val routes = new SessionRoutes(context.system).routes
    Server.start(routes, httpPort, context.system)
    Behaviors.empty
  }

}
