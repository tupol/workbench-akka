package org.tupol.akka.demo.test2

import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import com.typesafe.config.ConfigFactory

object TestApp extends App {

  val config = ConfigFactory.load()
  ActorSystem[Nothing](Guardian(), "Test", config)

}

object Guardian {
  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    val sharding = ClusterSharding(context.system)
    val TypeKey  = EntityTypeKey[Counter.Command]("Counter")

    val shardRegion: ActorRef[ShardingEnvelope[Counter.Command]] =
      sharding.init(Entity(TypeKey)(createBehavior = entityContext => Counter(entityContext.entityId)))

    Behaviors.empty
  }
}
