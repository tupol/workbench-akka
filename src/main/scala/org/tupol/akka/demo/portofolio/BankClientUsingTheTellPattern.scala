package org.tupol.akka.demo.portofolio

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import org.tupol.akka.demo.portofolio.Bank.{ CreatePortfolio, PortfolioCreated }
import org.tupol.akka.demo.portofolio.PortfolioActor.Buy

object BankClientUsingTheTellPattern {
  def apply(bank: ActorRef[CreatePortfolio]): Behavior[PortfolioCreated] =
    Behaviors.setup { context =>
      bank ! CreatePortfolio(context.self)
      Behaviors.receiveMessage {
        case PortfolioCreated(portfolio) =>
          portfolio ! Buy("AAPL", 100L)
          Behaviors.empty
      }
    }
}
