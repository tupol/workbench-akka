package org.tupol.akka.demo.bank2

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, StashBuffer }
import org.tupol.akka.demo.bank2.domain.{ BankDao, Client, ClientId }

import scala.concurrent.Future
import scala.util.{ Failure, Success }

object ClientActor {

  sealed trait Request
  sealed trait Response extends Request

  case class Get(replyTo: ActorRef[Response]) extends Request
  case class GetResponse(client: Client)      extends Response

  case class CreateClient(name: String, replyTo: ActorRef[Response]) extends Request
  case class ClientCreated(client: Client)                           extends Response

  case class FindClient(clientId: ClientId, replyTo: ActorRef[Response]) extends Request
  case class ClientFound(client: Client)                                 extends Response

  case class AddNewAccount(creditLimit: Long = 0, amount: Long = 0, replyTo: ActorRef[Response]) extends Request
  case class ClientAccountAdded(client: Client)                                                  extends Response

  case class Activate(replyTo: ActorRef[Response]) extends Request
  case class ClientActivated(client: Client)       extends Response

  case class Deactivate(replyTo: ActorRef[Response]) extends Request
  case class ClientDeactivated(client: Client)       extends Response

  case class Error(message: String, cause: Throwable = None.orNull) extends Exception(message, cause) with Response

  def apply(name: String, bankDao: BankDao, stashSize: Int): Behavior[Request] =
    Behaviors.withStash(stashSize) { buffer =>
      Behaviors.setup[Request] { context =>
        new ClientActor(context, buffer, bankDao).create(name)
      }
    }

  def apply(clientId: ClientId, bankDao: BankDao, stashSize: Int): Behavior[Request] =
    Behaviors.withStash(stashSize) { buffer =>
      Behaviors.setup[Request] { context =>
        new ClientActor(context, buffer, bankDao).find(clientId)
      }
    }
}

class ClientActor(
  context: ActorContext[ClientActor.Request],
  buffer: StashBuffer[ClientActor.Request],
  bankDao: BankDao
) {

  import ClientActor._
  implicit val ec = context.system.executionContext

  def create(name: String): Behavior[Request] = {
    context.pipeToSelf(bankDao.clientDao.create(name)) {
      case Success(client) => ClientCreated(client)
      case Failure(cause)  => Error(s"Failed to create client '$name'.", cause)
    }
    handlingInitialization()
//    Behaviors.receiveMessage {
//      case ClientCreated(client) =>
//        buffer.unstashAll(active(client))
//      case error: Error =>
//        throw error
//      case other =>
//        println(s"Stashing $other while creating")
//        buffer.stash(other)
//        Behaviors.same
//    }
  }

  def find(clientId: ClientId): Behavior[Request] = {
    context.pipeToSelf(bankDao.clientDao.findById(clientId)) {
      case Success(Some(client)) => ClientFound(client)
      case Success(None)         => Error(s"Client with id '$clientId' was not found.")
      case Failure(cause)        => Error(s"Failed to find client with id '$clientId'.", cause)
    }
    handlingInitialization()
//    Behaviors.receiveMessage {
//      case ClientFound(client) =>
//        buffer.unstashAll(active(client))
//      case error: Error =>
//        throw error
//      case other =>
//        println(s"Stashing $other while finding")
//        buffer.stash(other)
//        Behaviors.same
//    }
  }

  def active(client: Client): Behavior[Request] =
    Behaviors.receiveMessage {
      case Get(replyTo) =>
        replyTo ! GetResponse(client)
        Behaviors.same
      case Activate(replyTo) =>
        if (client.active) {
          replyTo ! Error(s"Failed to deactivate client ${client.label}; already active.")
          Behaviors.same
        } else {
          context.pipeToSelf(bankDao.clientDao.update(client.copy(active = true))) {
            case Success(client) => ClientActivated(client)
            case Failure(cause)  => Error(s"Failed to activate client ${client.label}", cause)
          }
          handlingRequest(replyTo)
        }
      case Deactivate(replyTo) =>
        if (!client.active) {
          replyTo ! Error(s"Failed to deactivate client ${client.label}; already inactive.")
          Behaviors.same
        } else {
          context.pipeToSelf(bankDao.clientDao.update(client.copy(active = false))) {
            case Success(client) => ClientDeactivated(client)
            case Failure(cause)  => Error(s"Failed to deactivate client ${client.label}.", cause)
          }
          handlingRequest(replyTo)
        }
      case AddNewAccount(creditLimit, amount, replyTo) =>
        // This should be transactional
        def addNewAccount(): Future[Client] =
          for {
            account       <- bankDao.accountDao.create(client.id, creditLimit, amount)
            updatedClient <- bankDao.clientDao.update(client.copy(accounts = client.accounts + account))
          } yield updatedClient
        context.pipeToSelf(addNewAccount()) {
          case Success(client) => ClientAccountAdded(client)
          case Failure(cause)  => Error(s"Failed to add a new account to ${client.label}", cause)
        }
        handlingRequest(replyTo)
    }

  def handlingRequest(replyTo: ActorRef[Response]): Behavior[Request] =
    Behaviors.receiveMessage {
      case response @ ClientAccountAdded(client) =>
        replyTo ! response
        buffer.unstashAll(active(client))
      case error: Error =>
        throw error
      case other =>
        println(s"Stashing $other while handlingRequest")
        buffer.stash(other)
        Behaviors.same
    }

  def handlingInitialization(): Behavior[Request] =
    Behaviors.receiveMessage {
      case ClientFound(client) =>
        buffer.unstashAll(active(client))
      case ClientCreated(client) =>
        buffer.unstashAll(active(client))
      case error: Error =>
        throw error
      case other =>
        println(s"Stashing $other while handlingInitialization")
        buffer.stash(other)
        Behaviors.same
    }

}
