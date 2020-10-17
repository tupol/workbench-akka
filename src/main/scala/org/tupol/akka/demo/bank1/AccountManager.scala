package org.tupol.akka.demo.bank1

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, StashBuffer }
import akka.actor.typed.{ ActorRef, Behavior }
import org.tupol.akka.demo.bank1.domain._

import scala.util.{ Failure, Success }

object AccountManager {

  sealed trait Command

  case class Create(creditLimit: Long = 0, amount: Long = 0, replyTo: ActorRef[Command]) extends Command
  case class Find(accountId: AccountId, replyTo: ActorRef[Command])                      extends Command

  private final case class AccountCreated(value: ActorRef[AccountActor.Command]) extends Command
  private final case class AccountFound(value: ActorRef[AccountActor.Command])   extends Command
  private final case class DaoError(cause: Throwable)                            extends Command
//  def apply(bankEngine: BankEngine, stashSize: Int = 100): Behavior[Command] = {
//      Behaviors.setup[Command] { context =>
//        new AccountManager(context, bankEngine).start()
//      }
//    }
}

class AccountManager(context: ActorContext[AccountManager.Command], bankEngine: BankEngine) {

  import AccountManager._
  implicit val ec = context.system.executionContext

//  private def start(): Behavior[Command] = {
//    running(Set[ActorRef[AccountActor.Command]]())
//  }
//  private def running(accounts: Set[ActorRef[AccountActor.Command]]): Behavior[Command] = {
//    Behaviors.receiveMessage {
//      case Create(creditLimit, amount, replyTo) =>
//        context.pipeToSelf(bankEngine.accountDao.create(creditLimit, amount)){
//          case Success(account) =>
//            val accountRef: ActorRef[AccountActor.Command] = context.spawn(AccountActor(account.id, bankEngine), account.id.toString)
//            AccountCreated(accountRef)
//          case Failure(cause) => DaoError(cause)
//        }
//        busy(accounts, replyTo)
//        bankEngine.accountDao.create(creditLimit, amount) onComplete {
//          case Success(account) =>
//            val accountRef: ActorRef[Command] = context.spawn(AccountActor(account.id, bankEngine), account.id.toString)
//            running(accounts)
//            replyTo ! AccountCreated(accountRef)
//          case Failure(t) => throw t
//        }
//      case Find(accountId, replyTo) =>
//        val accountRef = context.spawn(AccountActor(accountId, bankEngine), accountId.toString)
//        replyTo ! AccountFound(accountRef)
//        Behaviors.same
//    }
//  }

//  private def busy(accounts: Set[ActorRef[Command]], replyTo: ActorRef[AccountManager.Command]): Behavior[Command] = {
//    Behaviors.receiveMessage {
//
//    }
//  }

}
