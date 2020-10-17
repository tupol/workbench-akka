package org.tupol.akka.demo.test1

import akka.Done
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, StashBuffer }
import akka.actor.typed.{ ActorRef, Behavior }

import scala.concurrent.Future
import scala.util.{ Failure, Success }

trait DB {
  def create(value: String): Future[(String, String)]
  def update(id: String, value: String): Future[Done]
  def delete(id: String): Future[Done]
  def load(id: String): Future[String]
}

object DataAccess {
  sealed trait Command
  final case class Update(value: String, replyTo: ActorRef[String]) extends Command
  final case class Get(replyTo: ActorRef[String])                   extends Command
  private final case class InitialState(value: String)              extends Command
  private case class UpdateSuccess(summary: String)                 extends Command
  private final case class DBError(cause: Throwable)                extends Command

  def apply(id: String, db: DB): Behavior[Command] =
    Behaviors.withStash(100) { buffer =>
      Behaviors.setup[Command] { context =>
        new DataAccess(context, buffer, id, db).start()
      }
    }
}

class DataAccess(
  context: ActorContext[DataAccess.Command],
  buffer: StashBuffer[DataAccess.Command],
  id: String,
  db: DB
) {
  import DataAccess._

  private def start(): Behavior[Command] = {
    context.pipeToSelf(db.load(id)) {
      case Success(value) => InitialState(value)
      case Failure(cause) => DBError(cause)
    }

    Behaviors.receiveMessage {
      case InitialState(value) =>
        // now we are ready to handle stashed messages if any
        buffer.unstashAll(active(value))
      case DBError(cause) =>
        throw cause
      case other =>
        println(s"Stashing $other")
        // stash all other messages for later processing
        buffer.stash(other)
        Behaviors.same
    }
  }

  private def active(state: String): Behavior[Command] =
    Behaviors.receiveMessagePartial {
      case Get(replyTo) =>
        replyTo ! state
        Behaviors.same
      case Update(value, replyTo) =>
        context.pipeToSelf(db.update(id, value)) {
          case Success(_)     => UpdateSuccess(s"$id: $value")
          case Failure(cause) => DBError(cause)
        }
        busy(value, replyTo)
    }

  private def busy(state: String, replyTo: ActorRef[String]): Behavior[Command] =
    Behaviors.receiveMessage {
      case UpdateSuccess(summary) =>
        replyTo ! summary
        buffer.unstashAll(active(state))
      case DBError(cause) =>
        throw cause
      case other =>
        println(s"Stashing $other")
        buffer.stash(other)
        Behaviors.same
    }

}
