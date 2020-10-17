package org.tupol.akka.demo.portofolio

import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors

object BankMain {

  sealed trait Command
  final case class Start(clientName: String) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.info("Creation of the bank")
      val bank = context.spawn(Bank(), "bank")

      Behaviors.receiveMessage {
        case Start(clientName) =>
          context.log.info(s"Start a new client $clientName")
          val client: ActorRef[Bank.PortfolioCreated] = context.spawn(BankClientUsingTheTellPattern(bank), clientName)

          Behaviors.same
      }
    }

  def main(args: Array[String]): Unit = {

    val system: ActorSystem[BankMain.Command] = ActorSystem(BankMain(), "main")

    system ! Start("Alice")
    system ! Start("Bob")

  }
}
