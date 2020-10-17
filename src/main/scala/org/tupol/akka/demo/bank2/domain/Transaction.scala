package org.tupol.akka.demo.bank2.domain

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ ExecutionContext, Future }

case class TransactionId(value: UUID = UUID.randomUUID())

case class RejectionInfo(reason: String, rejectedAt: Instant = Instant.now())

sealed trait Transaction {
  def id: TransactionId
  def from: AccountId
  def to: AccountId
  def amount: Long
  def createdAt: Instant
  def completedAt: Option[Instant]
  def rejectionInfo: Option[RejectionInfo]
  def refundedAt: Option[Instant]
  final def completed: Boolean = completedAt.isDefined
  final def rejected: Boolean  = rejectionInfo.isDefined
  final def refunded: Boolean  = refundedAt.isDefined
}

case class StartTransaction(id: TransactionId, from: AccountId, to: AccountId, amount: Long) extends Transaction {
  require(amount > 0, "The transaction amount must be greater than 0")
  override val createdAt: Instant                   = Instant.now()
  override val completedAt: Option[Instant]         = None
  override val rejectionInfo: Option[RejectionInfo] = None
  override val refundedAt: Option[Instant]          = None
}
case class CompletedTransaction private (
  id: TransactionId,
  from: AccountId,
  to: AccountId,
  amount: Long,
  createdAt: Instant,
  completedAt: Option[Instant]
) extends Transaction {
  require(amount > 0, "The transaction amount must be greater than 0")
  override val rejectionInfo: Option[RejectionInfo] = None
  override val refundedAt: Option[Instant]          = None
}
object CompletedTransaction {
  def apply(startTransaction: StartTransaction): CompletedTransaction =
    CompletedTransaction(
      id = startTransaction.id,
      from = startTransaction.from,
      to = startTransaction.to,
      amount = startTransaction.amount,
      createdAt = startTransaction.createdAt,
      completedAt = Some(Instant.now())
    )
}
case class RejectedTransaction private (
  id: TransactionId,
  from: AccountId,
  to: AccountId,
  amount: Long,
  createdAt: Instant,
  completedAt: Option[Instant],
  rejectionInfo: Option[RejectionInfo]
) extends Transaction {
  override val refundedAt: Option[Instant] = None
}
object RejectedTransaction {
  def apply(transaction: StartTransaction, rejectionInfo: RejectionInfo): RejectedTransaction =
    RejectedTransaction(
      id = transaction.id,
      from = transaction.from,
      to = transaction.to,
      amount = transaction.amount,
      createdAt = transaction.createdAt,
      completedAt = Some(Instant.now()),
      rejectionInfo = Some(rejectionInfo)
    )
}
case class RefundedTransaction private (
  id: TransactionId,
  from: AccountId,
  to: AccountId,
  amount: Long,
  createdAt: Instant,
  completedAt: Option[Instant],
  rejectionInfo: Option[RejectionInfo],
  refundedAt: Option[Instant]
) extends Transaction
object RefundedTransaction {
  def apply(transaction: RejectedTransaction): RefundedTransaction =
    RefundedTransaction(
      id = transaction.id,
      from = transaction.from,
      to = transaction.to,
      amount = transaction.amount,
      createdAt = transaction.createdAt,
      completedAt = transaction.completedAt,
      rejectionInfo = transaction.rejectionInfo,
      refundedAt = Some(Instant.now())
    )
}

trait TransactionDao {
  def create(from: AccountId, to: AccountId, amount: Long): Future[Transaction]
  def find(id: TransactionId): Future[Option[Transaction]]
  def findAllStarted: Future[Iterable[StartTransaction]]
  def findAllCompleted: Future[Iterable[CompletedTransaction]]
  def findAllRejected: Future[Iterable[RejectedTransaction]]
  def findAllRefunded: Future[Iterable[RefundedTransaction]]
  def complete(transaction: Transaction): Future[Transaction]
  def reject(transaction: Transaction, rejectionInfo: RejectionInfo): Future[Transaction]
  def refund(transaction: Transaction): Future[Transaction]
}
class InMemoryTransactionDao(implicit ec: ExecutionContext) extends TransactionDao {

  private var transactions = Map.empty[TransactionId, Transaction]

  override def create(from: AccountId, to: AccountId, amount: Long): Future[StartTransaction] =
    Future {
      synchronized {
        val transaction = StartTransaction(TransactionId(), from, to, amount)
        transactions = transactions + (transaction.id -> transaction)
        transaction
      }
    }

  override def find(id: TransactionId): Future[Option[Transaction]] =
    Future {
      synchronized {
        transactions.get(id)
      }
    }
  override def complete(transaction: Transaction): Future[Transaction] =
    transaction match {
      case tx: StartTransaction =>
        Future {
          synchronized {
            val completedTransaction = CompletedTransaction(tx)
            transactions.updated(transaction.id, completedTransaction)
            completedTransaction
          }
        }
      case _ =>
        Future.failed(TransactionIllegalStateException(transaction.id))
    }
  override def reject(transaction: Transaction, rejectionInfo: RejectionInfo): Future[Transaction] =
    transaction match {
      case tx: StartTransaction =>
        Future {
          synchronized {
            val rejectedTransaction = RejectedTransaction(tx, rejectionInfo)
            transactions.updated(transaction.id, rejectedTransaction)
            rejectedTransaction
          }
        }
      case _ =>
        Future.failed(TransactionIllegalStateException(transaction.id))
    }

  override def refund(transaction: Transaction): Future[Transaction] =
    transaction match {
      case tx: RejectedTransaction =>
        Future {
          synchronized {
            val refundedTransaction = RefundedTransaction(tx)
            transactions.updated(transaction.id, refundedTransaction)
            refundedTransaction
          }
        }
      case _ =>
        Future.failed(TransactionIllegalStateException(transaction.id))
    }

  override def findAllStarted: Future[Iterable[StartTransaction]] =
    Future.successful(transactions.collect { case (_, tx: StartTransaction) => tx })

  override def findAllCompleted: Future[Iterable[CompletedTransaction]] =
    Future.successful(transactions.collect { case (_, tx: CompletedTransaction) => tx })

  override def findAllRejected: Future[Iterable[RejectedTransaction]] =
    Future.successful(transactions.collect { case (_, tx: RejectedTransaction) => tx })

  override def findAllRefunded: Future[Iterable[RefundedTransaction]] =
    Future.successful(transactions.collect { case (_, tx: RefundedTransaction) => tx })
}

case class TransactionIllegalStateException(id: TransactionId) extends Exception
