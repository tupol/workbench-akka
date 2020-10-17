package org.tupol.akka.demo.session

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorSystem, Scheduler }
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.tupol.akka.demo.session.SessionsGuardian.{ SessionNotFound, SessionResponse }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object SessionTest extends App {

  val config = ConfigFactory.load("application-simple.conf")
  implicit val system: ActorSystem[SessionsGuardian.Request] =
    ActorSystem(SessionsGuardian.applyWithInternalChildren(), "session-system", config)

  val sessionReceiver = ActorSystem(Behaviors.receiveMessage[Session] { message =>
    println(s"### Received $message")
    Behaviors.same
  }, "session-receiver")

  implicit val timeout: Timeout     = 3.seconds
  implicit val ec                   = system.executionContext
  implicit val scheduler: Scheduler = system.scheduler

  def createSessionActor(system: ActorSystem[SessionsGuardian.Request]): Future[SessionResponse] =
    system.ask[SessionsGuardian.Response](ref => SessionsGuardian.SpawnSession(ref)).map {
      case response: SessionResponse => response
      case _                         => throw new Exception("Something went wrong spawning a new session actor")
    }

  def findSessionActor(
    sessionId: SessionId
  )(system: ActorSystem[SessionsGuardian.Request]): Future[Option[SessionResponse]] =
    system.ask[SessionsGuardian.Response](ref => SessionsGuardian.FindSession(sessionId, ref)).map {
      case response: SessionResponse => Some(response)
      case SessionNotFound(_)        => None
      case _                         => throw new Exception("Something went wrong spawning a new session actor")
    }

  val session1 = Await.result(createSessionActor(system), timeout.duration)
  session1.ref ! SessionActor.Put("a", "b")
  session1.ref ! SessionActor.Get(sessionReceiver)
  Thread.sleep(1000)

  val sessionx = Await.result(findSessionActor(session1.sessionId)(system), timeout.duration).get
  sessionx.ref ! SessionActor.Put("c", "d")
  sessionx.ref ! SessionActor.Get(sessionReceiver)
  sessionx.ref ! SessionActor.Terminate
  Thread.sleep(1000)

  val sessiony = Await.result(findSessionActor(session1.sessionId)(system), timeout.duration)
  println(sessiony)

}
