package org.tupol.akka.demo

package object portofolio {

  case class Portfolio(stocks: Map[String, Stock]) {
    def buy(name: String, qty: Long): Portfolio = {
      val actuallyOwned: Stock = stocks.getOrElse(name, Stock(name, 0))
      copy(stocks + (name -> actuallyOwned.buy(qty)))
    }

    def sell(name: String, qty: Long): Portfolio = {
      val maybeStock = stocks.get(name)
      maybeStock.fold(this) { actuallyOwned =>
        copy(stocks + (name -> actuallyOwned.sell(qty)))
      }
    }
  }

  case class Stock(name: String, owned: Long) {
    def buy(qty: Long): Stock = copy(name, owned + qty)
    def sell(qty: Long): Stock =
      if (qty <= owned)
        copy(name, owned - qty)
      else
        this
  }

}
