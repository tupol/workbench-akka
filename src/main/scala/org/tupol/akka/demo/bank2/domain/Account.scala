package org.tupol.akka.demo.bank2.domain

import java.util.UUID

import scala.concurrent.{ ExecutionContext, Future }

case class AccountId(value: UUID = UUID.randomUUID())

case class Account(id: AccountId, clientId: ClientId, creditLimit: Long, amount: Long, active: Boolean = true) {
  require(amount >= -creditLimit, "The account amount can no be smaller than the credit limit")
}

trait AccountDao {
  def create(clientId: ClientId, creditLimit: Long = 0, amount: Long = 0): Future[Account]
  def findById(accountId: AccountId): Future[Option[Account]]
  def findByClientId(clientId: ClientId): Future[Iterable[Account]]
  def update(account: Account): Future[Account]
}

class InMemoryAccountDao(implicit ec: ExecutionContext) extends AccountDao {
  private var accounts = Map.empty[AccountId, Account]

  override def create(clientId: ClientId, creditLimit: Long = 0, amount: Long = 0): Future[Account] =
    Future {
      synchronized {
        val account = Account(AccountId(), clientId, creditLimit, amount)
        accounts = accounts + (account.id -> account)
        account
      }
    }

  override def findById(accountId: AccountId): Future[Option[Account]] =
    Future {
      synchronized {
        accounts.get(accountId)
      }
    }

  override def findByClientId(clientId: ClientId): Future[Iterable[Account]] =
    Future {
      synchronized {
        accounts.values.filter(_.clientId == clientId)
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
