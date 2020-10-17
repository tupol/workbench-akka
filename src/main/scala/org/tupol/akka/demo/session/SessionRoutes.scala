package org.tupol.akka.demo.session

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import akka.actor.typed.{ ActorRef, ActorSystem, Scheduler }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern._

import org.tupol.akka.demo.session.SessionsGuardian.{ SessionNotFound, SessionResponse }

import scala.util.{ Failure, Success }

class SessionRoutes(sessionsGuardian: ActorRef[SessionsGuardian.Request], system: ActorSystem[_]) {

  import org.json4s.{ DefaultFormats, Formats }
  import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

  implicit val formats: Formats = DefaultFormats
  implicit val serialization    = org.json4s.jackson.Serialization

  implicit val timeout: Timeout     = 3.seconds
  implicit val ec                   = system.executionContext
  implicit val scheduler: Scheduler = system.scheduler

  val routes: Route =
    pathPrefix("session") {
      path("create") {
        get {
          onComplete(createSessionActor) {
            case Success(value) => complete(s"${value.sessionId}")
            case Failure(error) => failWith(error)
          }
        }
      } ~
        pathPrefix(Segment) { sessionId =>
          path("find") {
            get {
              def process =
                for {
                  result <- findSessionActor(SessionId(sessionId))
                  session <- result match {
                              case Some(res) => res.ref.ask[Session](ref => SessionActor.Get(ref)).map(Some(_))
                              case None      => Future.successful(None)
                            }
                } yield (session)

              onComplete(process) {
                case Success(Some(session)) => complete(s"$session")
                case Success(None)          => complete(s"Session $sessionId not found")
                case Failure(error)         => failWith(error)
              }
            }
          } ~
            path("refresh") {
              get {
                def process =
                  for {
                    result <- findSessionActor(SessionId(sessionId))
                    _ <- result match {
                          case Some(res) => Future.successful(res.ref ! SessionActor.Refresh)
                          case None      => Future.failed(new Exception(s"Session $sessionId not found"))
                        }
                  } yield ()

                onComplete(process) {
                  case Success(_)     => complete(s"Session $sessionId refreshed")
                  case Failure(error) => failWith(error)
                }
              }
            } ~
            path("update") {
              post {
                entity(as[Record]) { record =>
                  def process =
                    for {
                      result <- findSessionActor(SessionId(sessionId))
                      _ <- result match {
                            case Some(res) => Future.successful(res.ref ! SessionActor.Put(record.key, record.value))
                            case None      => Future.failed(new Exception(s"Session $sessionId not found"))
                          }
                    } yield ()

                  onComplete(process) {
                    case Success(_)     => complete(s"Session $sessionId updated with $record")
                    case Failure(error) => failWith(error)
                  }
                }
              }
            } ~
            path("terminate") {
              get {
                def process =
                  for {
                    result <- findSessionActor(SessionId(sessionId))
                    _ <- result match {
                          case Some(res) => Future.successful(res.ref ! SessionActor.Terminate)
                          case None      => Future.failed(new Exception(s"Session $sessionId not found"))
                        }
                  } yield ()

                onComplete(process) {
                  case Success(_)     => complete(s"Session $sessionId terminated")
                  case Failure(error) => failWith(error)
                }
              }
            }
        }
    }

  def createSessionActor(
    implicit scheduler: Scheduler,
    timeout: Timeout,
    ec: ExecutionContext
  ): Future[SessionResponse] =
    sessionsGuardian.ask[SessionsGuardian.Response](ref => SessionsGuardian.SpawnSession(ref)).map {
      case response: SessionResponse => response
      case _                         => throw new Exception("Something went wrong spawning a new session actor")
    }

  def findSessionActor(
    sessionId: SessionId
  )(implicit scheduler: Scheduler, timeout: Timeout, ec: ExecutionContext): Future[Option[SessionResponse]] =
    sessionsGuardian.ask[SessionsGuardian.Response](ref => SessionsGuardian.FindSession(sessionId, ref)).map {
      case response: SessionResponse => Some(response)
      case SessionNotFound(_)        => None
      case _                         => throw new Exception("Something went wrong spawning a new session actor")
    }
}

case class Record(key: String, value: String)

case class SessionData(records: Set[Record])
