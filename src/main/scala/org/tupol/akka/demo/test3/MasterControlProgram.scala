package org.tupol.akka.demo.test3
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, PostStop }
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.actor.typed.receptionist.Receptionist.Register
import akka.actor.typed.scaladsl.Behaviors
import org.slf4j.Logger
import org.tupol.akka.demo.bank1.AccountActor

object MasterControlProgram {
  sealed trait Command
  final case class SpawnJob(name: String) extends Command
  final case class GetJob(name: String)   extends Command
  case object GracefulShutdown            extends Command

  // Predefined cleanup operation
  def cleanup(log: Logger): Unit = log.info("Cleaning up!")

  def apply(): Behavior[Command] =
    Behaviors
      .receive[Command] { (context, message) =>
        message match {
          case SpawnJob(jobName) =>
            context.log.info("Spawning job {}!", jobName)
            val actorRef: ActorRef[Job.Command] = context.spawn(Job(jobName), name = jobName)
            val key                             = ServiceKey[Job.Command](jobName)
            context.system.receptionist ! Receptionist.register(key, actorRef)
            Behaviors.same
          case GracefulShutdown =>
            context.log.info("Initiating graceful shutdown...")
            // perform graceful stop, executing cleanup before final system termination
            // behavior executing cleanup is passed as a parameter to Actor.stopped
            Behaviors.stopped { () =>
              cleanup(context.system.log)
            }
        }
      }
      .receiveSignal {
        case (context, PostStop) =>
          context.log.info("Master Control Program stopped")
          Behaviors.same
      }
}

object Job {
  sealed trait Command

  def apply(name: String): Behavior[Command] =
    Behaviors.receiveSignal[Command] {
      case (context, PostStop) =>
        context.log.info("Worker {} stopped", name)
        Behaviors.same
    }
}
