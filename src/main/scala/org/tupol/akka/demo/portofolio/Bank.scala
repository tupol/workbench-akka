package org.tupol.akka.demo.portofolio

import java.util.UUID

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import org.tupol.akka.demo.portofolio.PortfolioActor.PortfolioCommand

object Bank {
  sealed trait Command
  final case class CreatePortfolio(client: ActorRef[PortfolioCreated]) extends Command
  final case class PortfolioCreated(portfolio: ActorRef[PortfolioCommand])

  def apply(): Behavior[CreatePortfolio] =
    Behaviors.receive { (context, message) =>
      val replyTo = context.spawn(PortfolioActor(), UUID.randomUUID().toString)
      message.client ! PortfolioCreated(replyTo)
      Behaviors.same
    }
}
