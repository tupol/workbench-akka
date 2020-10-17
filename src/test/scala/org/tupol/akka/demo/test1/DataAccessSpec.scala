package org.tupol.akka.demo.test1

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.{ after => aafter }
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future
import scala.concurrent.duration._

class DataAccessSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  class TestDB extends DB {

    override def update(id: String, value: String): Future[Done] = aafter(2.seconds)(Future.successful(Done))

    override def load(id: String): Future[String] = Future.successful(id)

    override def create(value: String): Future[(String, String)] = ???

    override def delete(id: String): Future[Done] = ???
  }

  "DataAccess" must {
    "blah" in {
      val replyProbe  = createTestProbe[String]()
      val testedActor = spawn(DataAccess.apply("some-id", new TestDB()))
      testedActor ! DataAccess.Update("123", replyProbe.ref)
      testedActor ! DataAccess.Get(replyProbe.ref)
      testedActor ! DataAccess.Get(replyProbe.ref)
      testedActor ! DataAccess.Update("234", replyProbe.ref)
      testedActor ! DataAccess.Get(replyProbe.ref)
      testedActor ! DataAccess.Get(replyProbe.ref)
      testedActor ! DataAccess.Update("345", replyProbe.ref)
      testedActor ! DataAccess.Get(replyProbe.ref)
      testedActor ! DataAccess.Get(replyProbe.ref)
//      replyProbe.expectMessageType[String]
      println(replyProbe.receiveMessage())
      println(replyProbe.receiveMessage())
      println(replyProbe.receiveMessage())
      println(replyProbe.receiveMessage())
      println(replyProbe.receiveMessage())
      println(replyProbe.receiveMessage())
      println(replyProbe.receiveMessage())
      println(replyProbe.receiveMessage())
      println(replyProbe.receiveMessage())

    }
  }

}
