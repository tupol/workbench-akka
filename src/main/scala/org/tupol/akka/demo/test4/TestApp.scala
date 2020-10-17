package org.tupol.akka.demo.test4

import akka.actor.typed.{ ActorSystem, Scheduler }
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object TestApp extends App {

  import akka.actor.typed.scaladsl.AskPattern._
  import akka.util.Timeout

  implicit val timeout: Timeout = 3.seconds

  val cookieFabric: ActorSystem[CookieFabric.GiveMeCookies] = ActorSystem(CookieFabric(), "cookie-fabric")

  implicit val ec: ExecutionContextExecutor = cookieFabric.executionContext
  implicit val scheduler: Scheduler         = cookieFabric.scheduler

  val result: Future[CookieFabric.Reply] = cookieFabric.ask(ref => CookieFabric.GiveMeCookies(3, ref))

  result.onComplete {
    case Success(CookieFabric.Cookies(count))         => println(s"Yay, $count cookies!")
    case Success(CookieFabric.InvalidRequest(reason)) => println(s"No cookies for me. $reason")
    case Failure(ex)                                  => println(s"Boo! didn't get cookies: ${ex.getMessage}")
  }
}
