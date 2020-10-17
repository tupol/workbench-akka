package org.tupol.akka.demo.session

import akka.actor.typed.{ ActorRef, Behavior, Terminated }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }

import scala.concurrent.duration._
import scala.util.{ Success, Try }

object SessionsGuardian {
  sealed trait Request
  case class SpawnSession(replyTo: ActorRef[Response])                      extends Request
  case class FindSession(sessionId: SessionId, replyTo: ActorRef[Response]) extends Request

  sealed trait Response
  case class SessionResponse(sessionId: SessionId, ref: ActorRef[SessionActor.Command]) extends Response
  case class SessionNotFound(sessionId: SessionId)                                      extends Response

  def applyWithCustomChildrenManagement(
    idleTime: FiniteDuration = 60.seconds,
    maxTime: FiniteDuration = 600.seconds
  ): Behavior[Request] =
    Behaviors.setup { context =>
      def active(sessions: Map[SessionId, ActorRef[SessionActor.Command]] = Map()): Behavior[Request] =
        Behaviors
          .receiveMessage[Request] {
            case SpawnSession(replyTo) =>
              val sessionId    = SessionId()
              val sessionActor = SessionActor(sessionId.toString, idleTime, maxTime)
              val ref          = context.spawn(sessionActor, sessionId.toString)
              context.watch(ref)
              replyTo ! SessionResponse(sessionId, ref)
              active(sessions + (sessionId -> ref))
            case FindSession(sessionId, replyTo) =>
              sessions.get(sessionId) match {
                case Some(ref) => replyTo ! SessionResponse(sessionId, ref)
                case None      => replyTo ! SessionNotFound(sessionId)
              }
              Behaviors.same
          }
          .receiveSignal {
            case (context, Terminated(ref)) =>
              sessions.filter(_._2 == ref).toSeq match {
                case Nil =>
                  context.log.warn("Weird, somehow we can not find this one")
                  active(sessions)
                case (id, ref) +: Nil =>
                  context.log.info(s"Session $id was terminated")
                  active(sessions.filterNot(_._2 == ref))
                case _ =>
                  context.log.warn("Weird, somehow we found more than one actor ref; removing all")
                  active(sessions.filterNot(_._2 == ref))
              }
            case _ =>
              active(sessions)
          }

      active()
    }

  def applyWithInternalChildren(
    idleTime: FiniteDuration = 60.seconds,
    maxTime: FiniteDuration = 600.seconds
  ): Behavior[Request] =
    Behaviors.setup { context =>
      applyWithInternalChildrenAndContext(context, idleTime, maxTime)
    }

  def applyWithInternalChildrenAndContext(
    context: ActorContext[Request],
    idleTime: FiniteDuration = 60.seconds,
    maxTime: FiniteDuration = 600.seconds
  ): Behavior[Request] = {
    def active(): Behavior[Request] =
      //        val children: Iterable[ActorRef[SessionActor.Command]] =
      //          context.children.map(child => Try(child.unsafeUpcast[SessionActor.Command])).collect{case Success(c) => c}
      //
      //        println(s"### Children: ${children.size}")
      //        context.children.foreach{ child =>
      //          println(s"   ### ${child.path}")
      //        }
      Behaviors
        .receiveMessage[Request] {
          case SpawnSession(replyTo) =>
            val sessionId    = SessionId()
            val sessionActor = SessionActor(sessionId.toString, idleTime, maxTime)
            val ref          = context.spawn(sessionActor, sessionId.toString)
            context.watch(ref)
            replyTo ! SessionResponse(sessionId, ref)
            Behaviors.same
          case FindSession(sessionId, replyTo) =>
            context.child(sessionId.toString) match {
              case Some(ref) => replyTo ! SessionResponse(sessionId, ref.unsafeUpcast)
              case None      => replyTo ! SessionNotFound(sessionId)
            }
            Behaviors.same
        }
    active()
  }
}
