package org.tupol.akka.demo.portofolio

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object PortfolioActor {
  sealed trait PortfolioCommand
  final case class Buy(stock: String, quantity: Long)  extends PortfolioCommand
  final case class Sell(stock: String, quantity: Long) extends PortfolioCommand

  def apply(): Behavior[PortfolioCommand] =
    portfolio(Portfolio(Map.empty))

  private def portfolio(stocks: Portfolio): Behavior[PortfolioCommand] =
    Behaviors receiveMessage {
      case Buy(stock, qty) =>
        portfolio(stocks.buy(stock, qty))
      case Sell(stock, qty) =>
        portfolio(stocks.sell(stock, qty))
    }
}
