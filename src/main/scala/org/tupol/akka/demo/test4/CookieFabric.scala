package org.tupol.akka.demo.test4

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors

object CookieFabric {
  sealed trait Command
  case class GiveMeCookies(count: Int, replyTo: ActorRef[Reply]) extends Command

  sealed trait Reply
  case class Cookies(count: Int)            extends Reply
  case class InvalidRequest(reason: String) extends Reply

  def apply(): Behaviors.Receive[CookieFabric.GiveMeCookies] =
    Behaviors.receiveMessage { message =>
      if (message.count >= 5)
        message.replyTo ! InvalidRequest("Too many cookies.")
      else
        message.replyTo ! Cookies(message.count)
      Behaviors.same
    }
}
