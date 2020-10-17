package org.tupol.akka.demo.bank1

import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, StashBuffer }
import org.tupol.akka.demo.bank1.AccountActor.Command
import org.tupol.akka.demo.bank1.domain.{ Account, AccountId, BankEngine, BankError, Transaction }

import scala.util.{ Failure, Success }

object AccountActor {

  sealed trait Command

  case class Deactivate(replyTo: ActorRef[Account])                              extends Command
  case class Activate(replyTo: ActorRef[Account])                                extends Command
  case class UpdateCreditLimit(newCreditLimit: Long, replyTo: ActorRef[Account]) extends Command
  case class Get(replyTo: ActorRef[Account])                                     extends Command

  case class Pay(to: AccountId, amount: Long, replyTo: ActorRef[Transaction]) extends Command {
    require(amount > 0, "The amount must be greater than 0")
  }
  case class Receive(transaction: Transaction, replyTo: ActorRef[Transaction]) extends Command

  private final case class InitialState(value: Account)                           extends Command
  private case class AccountUpdateSuccess(value: Account)                         extends Command
  private case class TransactionSuccess(value: Account, transaction: Transaction) extends Command
  private case class TransactionFailure(transaction: Transaction)                 extends Command
  private final case class DaoError(cause: Throwable)                             extends Command
  case class LogicError(cause: Throwable)                                         extends Command
  case class AccountNotFound(id: AccountId)                                       extends IllegalStateException(s"Account not found: $id")

  def apply(id: AccountId, bankEngine: BankEngine, stashSize: Int = 100): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      apply(context, id, bankEngine, stashSize)
    }

  def apply(context: ActorContext[Command], id: AccountId, bankEngine: BankEngine, stashSize: Int): Behavior[Command] =
    Behaviors.withStash(stashSize) { buffer =>
      new AccountActor(context, buffer, id, bankEngine).start()
    }

}

class AccountActor(
  context: ActorContext[AccountActor.Command],
  buffer: StashBuffer[AccountActor.Command],
  id: AccountId,
  bankEngine: BankEngine
) {

  import AccountActor._

  private def start(): Behavior[Command] = {
    context.pipeToSelf(bankEngine.accountDao.find(id)) {
      case Success(Some(value)) =>
        val key = ServiceKey[AccountActor.Command](id.toString)
        context.system.receptionist ! Receptionist.register(key, context.self)
        InitialState(value)
      case Success(None)  => DaoError(AccountNotFound(id))
      case Failure(cause) => DaoError(cause)
    }

    Behaviors.receiveMessage {
      case InitialState(account) =>
        // now we are ready to handle stashed messages if any
        buffer.unstashAll(active(account))
      case DaoError(cause) =>
        throw cause
      case other =>
        println(s"Stashing $other")
        // stash all other messages for later processing
        buffer.stash(other)
        Behaviors.same
    }
  }

  private def active(account: Account): Behavior[Command] =
    Behaviors.receiveMessagePartial {
      case Get(replyTo) =>
        replyTo ! account
        Behaviors.same
      case Deactivate(replyTo) =>
        if (account.active) {
          context.pipeToSelf(bankEngine.accountDao.update(account.copy(active = false))) {
            case Success(account) => AccountUpdateSuccess(account)
            case Failure(cause)   => DaoError(cause)
          }
          busyWithAccount(replyTo)
        } else {
          replyTo ! account
          Behaviors.same
        }
      case Activate(replyTo) =>
        if (account.active) {
          replyTo ! account
          Behaviors.same
        } else {
          context.pipeToSelf(bankEngine.accountDao.update(account.copy(active = true))) {
            case Success(account) => AccountUpdateSuccess(account)
            case Failure(cause)   => DaoError(cause)
          }
          busyWithAccount(replyTo)
        }
      case UpdateCreditLimit(newCreditLimit, replyTo) =>
        context.pipeToSelf(bankEngine.accountDao.update(account.copy(creditLimit = newCreditLimit))) {
          case Success(account) => AccountUpdateSuccess(account)
          case Failure(cause)   => DaoError(cause)
        }
        busyWithAccount(replyTo)
      case Pay(to, amount, replyTo) =>
        context.pipeToSelf(bankEngine.transfer(account, to, amount)) {
          case Success((a, t)) => TransactionSuccess(a, t)
          case Failure(cause)  => DaoError(cause)
        }
        busyWithTransaction(replyTo)
      case Receive(transaction, replyTo) =>
        context.pipeToSelf(bankEngine.execute(transaction)) {
          case Success((t, Some(a))) => TransactionSuccess(a, t)
          case Success((t, None)) =>
            println(t)
            LogicError(BankError("Something went wrong; account could not be found."))
          case Failure(cause) => DaoError(cause)
        }
        busyWithTransaction(replyTo)
    }

  private def busyWithAccount(replyTo: ActorRef[Account]): Behavior[Command] =
    Behaviors.receiveMessage {
      case AccountUpdateSuccess(account) =>
        replyTo ! account
        buffer.unstashAll(active(account))
      case DaoError(cause) =>
        throw cause
      case other =>
        println(s"Stashing $other")
        buffer.stash(other)
        Behaviors.same
    }

  private def busyWithTransaction(replyTo: ActorRef[Transaction]): Behavior[Command] =
    Behaviors.receiveMessage {
      case TransactionSuccess(account, transaction) =>
        replyTo ! transaction
        buffer.unstashAll(active(account))
      case DaoError(cause) =>
        throw cause
      case LogicError(cause) =>
        throw cause
      case other =>
        println(s"Stashing $other")
        buffer.stash(other)
        Behaviors.same
    }

}
