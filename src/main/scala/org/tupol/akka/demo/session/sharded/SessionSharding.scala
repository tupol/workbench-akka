package org.tupol.akka.demo.session.sharded

import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }

import scala.concurrent.duration._
import org.tupol.akka.demo.session._

object SessionSharding {

  val SessionTypeKey = EntityTypeKey[SessionActor.Command]("Sessions")

  def initializeSharding(
    clusterSharding: ClusterSharding,
    idleTime: FiniteDuration = 60.seconds,
    maxTime: FiniteDuration = 600.seconds
  ): ActorRef[ShardingEnvelope[SessionActor.Command]] =
    clusterSharding.init(Entity(SessionTypeKey) { entityContext =>
      SessionActor(entityContext.entityId, idleTime, maxTime)
    })

}
