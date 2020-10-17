package org.tupol.akka.demo.bank1.domain

import scala.concurrent.{ ExecutionContext, Future }

class BankEngine(val accountDao: AccountDao, val transactionDao: TransactionDao)(implicit ec: ExecutionContext) {

  def transfer(from: Account, to: AccountId, amount: Long): Future[(Account, Transaction)] = {
    println("STARTING TRANSACTION")
    if (from.amount < amount) Future.failed(InsufficientFunds(from.id))
    else if (!from.active) Future.failed(InactiveAccount(from.id))
    else
      for {
        //-- Transaction start
        updatedAccount <- accountDao.update(from.copy(amount = from.amount - amount))
        _              = println(s"UPDATED ACCOUNT = $updatedAccount")
        transaction    <- transactionDao.create(from.id, to, amount)
        _              = println(s"TRANSACTION = $transaction")
        //-- Transaction end
      } yield (updatedAccount, transaction)
  }

  def execute(transaction: Transaction): Future[(Transaction, Option[Account])] = {
    println(s"EXECUTING TRANSACTION: $transaction")
    transaction match {
      case tx: CompletedTransaction => Future.failed(TransactionAlreadyCompleted(tx.id))
      case tx: RefundedTransaction  => Future.failed(TransactionAlreadyRefunded(tx.id))
      case tx: StartTransaction =>
        accountDao.find(transaction.to).flatMap {
          case Some(account) =>
            if (account.active) {
              for {
                //-- Transaction start
                updatedAccount       <- accountDao.update(account.copy(amount = account.amount + tx.amount))
                completedTransaction <- transactionDao.complete(tx)
                //-- Transaction end
              } yield ((completedTransaction, Some(updatedAccount)))
            } else {
              transactionDao
                .reject(tx, RejectionInfo("Target account is inactive"))
                .map((_, None))
            }
          case None =>
            transactionDao
              .reject(tx, RejectionInfo("Target account is unknown"))
              .map((_, None))

        }
      case tx: RejectedTransaction =>
        accountDao.find(transaction.to).flatMap {
          case Some(account) =>
            if (account.active) {
              for {
                //-- Transaction start
                updatedAccount      <- accountDao.update(account.copy(amount = account.amount + tx.amount))
                refundedTransaction <- transactionDao.refund(tx)
                //-- Transaction end
              } yield ((refundedTransaction, Some(updatedAccount)))
            } else {
              Future
                .failed(TransactionRefundError(tx.id, "The source account is no longer active. ZOMBIE TRANSACTION!!!"))
            }
          case None =>
            Future.failed(TransactionRefundError(tx.id, "The source account is unknown. ZOMBIE TRANSACTION!!!"))
        }
    }
  }
}

case class BankError(message: String) extends Exception(message)

case class InsufficientFunds(id: AccountId) extends Exception

case class InactiveAccount(id: AccountId) extends Exception

case class UnknownAccount(id: AccountId) extends Exception

case class TransactionAlreadyCompleted(id: TransactionId) extends Exception

case class TransactionAlreadyRefunded(id: TransactionId) extends Exception

case class TransactionRefundError(id: TransactionId, message: String = "") extends Exception(message)
