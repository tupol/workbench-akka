package org.tupol.akka.demo.session

import java.time.Instant
import java.util.UUID

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, TimerScheduler }

import scala.concurrent.duration._

case class SessionId(value: String = UUID.randomUUID().toString) {
  override def toString: String = value
}

case class Session(
  id: SessionId,
  state: Map[String, String] = Map(),
  createdAt: Instant = Instant.now(),
  lastCheckAt: Instant = Instant.now()
) extends SerializableMessage

object SessionActor {

  sealed trait Command            extends SerializableMessage
  sealed trait InteractiveCommand extends Command
  sealed trait TerminateCommand   extends Command

  case object Refresh                        extends InteractiveCommand
  case class Get(replyTo: ActorRef[Session]) extends InteractiveCommand
  case class Put(key: String, value: String) extends InteractiveCommand

  case object Terminate       extends TerminateCommand
  private case object Idled   extends TerminateCommand
  private case object Expired extends TerminateCommand

  private case object IdleTimerKey
  private case object TotalTimerKey

  def apply(
    sessionId: SessionId,
    idleTime: FiniteDuration,
    maxTime: FiniteDuration
  ): Behavior[Command] =
    apply(sessionId.toString, idleTime, maxTime)

  def apply(
    entityId: String = SessionId().toString,
    idleTime: FiniteDuration = 30.seconds,
    maxTime: FiniteDuration = 120.seconds
  ): Behavior[Command] =
    Behaviors.withTimers[SessionActor.Command] { timerScheduler =>
      new SessionActor(timerScheduler, SessionId(entityId), idleTime, maxTime).start()
    }

}

class SessionActor(
  timerScheduler: TimerScheduler[SessionActor.Command],
  id: SessionId,
  idleTime: FiniteDuration = 30.seconds,
  maxTime: FiniteDuration = 120.seconds
) {

  import SessionActor._

  private def refreshSession(session: Session) = session.copy(lastCheckAt = Instant.now())
  private def resetIdleClock                   = timerScheduler.startSingleTimer(IdleTimerKey, Idled, idleTime)

  private def start(): Behavior[Command] = {
    timerScheduler.startSingleTimer(IdleTimerKey, Idled, idleTime)
    timerScheduler.startSingleTimer(TotalTimerKey, Expired, maxTime)
    active(Session(id))
  }

  private def active(session: Session): Behavior[Command] =
    Behaviors.receive[Command] { (context, message) =>
      message match {
        case _: TerminateCommand =>
          message match {
            case Terminate => context.log.info(s"Terminating session ${session.id} on command.")
            case Idled     => context.log.info(s"Terminating session ${session.id} because it became idle.")
            case Expired   => context.log.info(s"Terminating session ${session.id} because it expired.")
          }
          Behaviors.stopped
        case _: InteractiveCommand =>
          resetIdleClock
          val refreshedSession = refreshSession(session)
          val newSession = message match {
            case Refresh =>
              context.log.info(s"Refreshing session ${session.id}.")
              refreshedSession
            case Put(key, value) =>
              context.log.info(s"Updating session ${session.id}.")
              refreshSession(session).copy(state = session.state + (key -> value))
            case Get(replyTo) =>
              context.log.info(s"Delivering session ${session.id}.")
              replyTo ! refreshedSession
              refreshedSession
          }
          active(newSession)
      }
    }
}
