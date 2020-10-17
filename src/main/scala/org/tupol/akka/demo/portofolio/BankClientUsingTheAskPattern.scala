package org.tupol.akka.demo.portofolio

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import org.tupol.akka.demo.portofolio.Bank.CreatePortfolio
import org.tupol.akka.demo.portofolio.PortfolioActor.Buy
import scala.concurrent.duration._

import scala.util.{ Failure, Success }

object BankClientUsingTheAskPattern {
  def apply(bank: ActorRef[CreatePortfolio]): Behavior[Unit] =
    Behaviors.setup { context =>
      implicit val timeout: Timeout = 3.seconds
      context.ask(bank, CreatePortfolio) {
        case Success(message) =>
          context.log.info("Portfolio received")
          message.portfolio ! Buy("AAPL", 100L)
        case Failure(_) => context.log.info("Portfolio received")
      }
      Behaviors.ignore[Unit]
    }
}
