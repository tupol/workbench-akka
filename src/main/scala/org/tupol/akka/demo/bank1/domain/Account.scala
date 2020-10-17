package org.tupol.akka.demo.bank1.domain

import java.util.UUID

import scala.concurrent.{ ExecutionContext, Future }

case class AccountId(value: UUID = UUID.randomUUID())

case class Account(id: AccountId, creditLimit: Long, amount: Long, active: Boolean = true) {
  require(amount >= -creditLimit, "The account amount can no be smaller than the credit limit")
}

case class AccountGroup(id: UUID, accounts: Set[Account])

trait AccountDao {
  def create(creditLimit: Long = 0, amount: Long = 0): Future[Account]
  def find(id: AccountId): Future[Option[Account]]
  def update(account: Account): Future[Account]
}

class InMemoryAccountDao(implicit ec: ExecutionContext) extends AccountDao {
  private var accounts = Map.empty[AccountId, Account]

  override def create(creditLimit: Long = 0, amount: Long = 0): Future[Account] =
    Future {
      synchronized {
        val account = Account(AccountId(), creditLimit, amount)
        accounts = accounts + (account.id -> account)
        account
      }
    }

  override def find(id: AccountId): Future[Option[Account]] =
    Future {
      synchronized {
        accounts.get(id)
      }
    }

  override def update(account: Account): Future[Account] =
    Future {
      synchronized {
        accounts = accounts.updated(account.id, account)
      }
      account
    }

}
