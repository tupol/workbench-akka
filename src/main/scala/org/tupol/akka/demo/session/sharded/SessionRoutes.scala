package org.tupol.akka.demo.session.sharded

import scala.concurrent.duration._
import akka.actor.typed.{ ActorRef, ActorSystem, Scheduler }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import org.json4s.ext.{ JavaTimeSerializers, JavaTypesSerializers }
import org.tupol.akka.demo.session.{ Record, Session, SessionActor, SessionId }

import scala.util.{ Failure, Success, Try }

class SessionRoutes(
  system: ActorSystem[_],
  idleTime: FiniteDuration = 60.seconds,
  maxTime: FiniteDuration = 600.seconds
) {

  import org.json4s.{ DefaultFormats, Formats }
  import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

  implicit val formats: Formats = DefaultFormats ++ JavaTypesSerializers.all ++ JavaTimeSerializers.all
  implicit val serialization    = org.json4s.jackson.Serialization

  implicit val timeout: Timeout     = 3.seconds
  implicit val ec                   = system.executionContext
  implicit val scheduler: Scheduler = system.scheduler

  private val sharding: ClusterSharding = ClusterSharding(system)
  // This is very important to describe how the instances will be created inside the system
  SessionSharding.initializeSharding(sharding)

  val routes: Route =
    pathPrefix("session") {
      path("create") {
        get {
          val newSessionId = SessionId()
          sharding
            .entityRefFor(SessionSharding.SessionTypeKey, newSessionId.toString)
          complete(newSessionId)
        }
      } ~
        pathPrefix(Segment) { sessionId =>
          path("find") {
            get {
              onComplete {
                // shardRegion.ask[Session](ref => ShardingEnvelope(sessionId, SessionActor.Get(ref)))
                // Working alternative
                sharding
                  .entityRefFor(SessionSharding.SessionTypeKey, sessionId)
                  .ask[Session](ref => SessionActor.Get(ref))
              } {
                case Success(session) => complete(session)
                case Failure(error)   => failWith(error)
              }
            }
          } ~
            path("refresh") {
              get {
                (Try {
                  sharding
                    .entityRefFor(SessionSharding.SessionTypeKey, sessionId)
                    .tell(SessionActor.Refresh)
                }) match {
                  case Success(session) => complete((s"Session $sessionId refreshed"))
                  case Failure(error)   => failWith(error)
                }
              }
            } ~
            path("terminate") {
              get {
                (Try {
                  sharding
                    .entityRefFor(SessionSharding.SessionTypeKey, sessionId)
                    .tell(SessionActor.Terminate)
                }) match {
                  case Success(session) => complete((s"Session $sessionId was terminated"))
                  case Failure(error)   => failWith(error)
                }
              }
            } ~
            path("update") {
              post {
                entity(as[Record]) { record =>
                  sharding
                    .entityRefFor(SessionSharding.SessionTypeKey, sessionId)
                    .tell(SessionActor.Put(record.key, record.value))
                  complete(s"Session $sessionId was updated with record $record")
                }
              }
            }
        }
    }
}
