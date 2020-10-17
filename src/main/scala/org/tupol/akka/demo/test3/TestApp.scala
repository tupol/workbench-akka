package org.tupol.akka.demo.test3

import akka.actor.typed.ActorSystem
import akka.actor.typed.receptionist.Receptionist

import scala.concurrent.Await
import scala.concurrent.duration._

object TestApp extends App {
  import MasterControlProgram.SpawnJob
  import MasterControlProgram.GracefulShutdown

  val system: ActorSystem[MasterControlProgram.Command] = ActorSystem(MasterControlProgram(), "B7700")

  system ! SpawnJob("a")
  system ! SpawnJob("b")

  Thread.sleep(100)

  // gracefully stop the system
  system ! GracefulShutdown

  Thread.sleep(100)

  Await.result(system.whenTerminated, 3.seconds)
}
