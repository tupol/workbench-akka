package org.tupol.akka.demo.bank1

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, StashBuffer }
import akka.actor.typed.{ ActorRef, Behavior }
import org.tupol.akka.demo.bank1.domain._

import scala.util.{ Failure, Success }

object TransactionManager {

  sealed trait Command

  case class Execute(transaction: Transaction, replyTo: ActorRef[Transaction]) extends Command

  private final case class DaoError(cause: Throwable)                             extends Command
  case class LogicError(cause: Throwable)                                         extends Command
  private case class TransactionSuccess(value: Account, transaction: Transaction) extends Command
  private case class TransactionFailure(transaction: Transaction)                 extends Command

  def apply(bankEngine: BankEngine): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      new TransactionManager(context, bankEngine).active()
    }

}

class TransactionManager(context: ActorContext[TransactionManager.Command], bankEngine: BankEngine) {

  import TransactionManager._

  private def active(): Behavior[Command] =
    Behaviors.receiveMessagePartial {
      case Execute(transaction, replyTo) =>
        context.pipeToSelf(bankEngine.execute(transaction)) {
          case Success((t, Some(a))) => TransactionSuccess(a, t)
          case Success((t, None)) =>
            println(t)
            LogicError(BankError("Something went wrong; account could not be found."))
          case Failure(cause) => DaoError(cause)
        }
        busy(replyTo)
    }

  private def busy(replyTo: ActorRef[Transaction]): Behavior[Command] =
    Behaviors.receiveMessage {
      case TransactionSuccess(account, transaction) =>
        context.system.receptionist
        replyTo ! transaction
        active()
      case DaoError(cause) =>
        throw cause
      case LogicError(cause) =>
        throw cause
    }

}
