package org.tupol.akka.demo.bank2

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, StashBuffer }
import org.tupol.akka.demo.bank2.domain.{ Account, AccountId, BankDao }

import scala.util.{ Failure, Success }

object AccountActor {

  sealed trait Command
  case class Create(creditLimit: Long = 0, amount: Long = 0, replyTo: ActorRef[Command]) extends Command
  case class Find(accountId: AccountId, replyTo: ActorRef[Command])                      extends Command
  case class Deactivate(replyTo: ActorRef[Command])                                      extends Command
  case class Activate(replyTo: ActorRef[Command])                                        extends Command

  private case class AccountCreated(account: Account)       extends Command
  private case class AccountUpdated(account: Account)       extends Command
  private case class AccountFound(account: Option[Account]) extends Command

  case class AccountCreationError(cause: Throwable) extends Exception("Failed to create account.", cause) with Command
  case class AccountNotFoundException(accountId: AccountId)
      extends IllegalStateException(s"Account Not Found: $accountId")
      with Command

}

class AccountActor(
  context: ActorContext[AccountActor.Command],
  buffer: StashBuffer[AccountActor.Command],
  bankDao: BankDao
) {

  import AccountActor._

  private def start(): Behavior[Command] =
    running()

  private def running(): Behavior[Command] =
    Behaviors.receiveMessagePartial {
//      case Create(creditLimit, amount, replyTo) =>
//              context.pipeToSelf(bankEngine.accountDao.create(creditLimit, amount)){
//                case Success(account) => AccountCreated(account)
//                case Failure(cause) => AccountCreationError(cause)
//              }
      case _ =>
        busy()
    }

  private def busy(): Behavior[Command] =
    Behaviors.receiveMessage {
      case AccountCreated(account) =>
        buffer.unstashAll(running())
      case AccountCreationError(cause) =>
        throw cause
      case other =>
        println(s"Stashing $other")
        buffer.stash(other)
        Behaviors.same
    }
}
