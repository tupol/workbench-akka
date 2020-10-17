package org.tupol.akka.demo.bank2

import akka.actor.testkit.typed.scaladsl.{ BehaviorTestKit, ScalaTestWithActorTestKit, TestInbox, TestProbe }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import org.tupol.akka.demo.bank2.domain._

import scala.util.Try

class ClientActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  import testKit.internalSystem.executionContext

  "ClientActor" must {
    "create a new actor" in {
      val bankDao = new BankDao(new InMemoryClientDao, new InMemoryAccountDao(), new InMemoryTransactionDao)

      val replyProbe  = createTestProbe[ClientActor.Response]
      val testClient1 = spawn(ClientActor("John", bankDao, 100))
      testClient1 ! ClientActor.Get(replyProbe.ref)

      println(replyProbe.receiveMessage())
    }

    "fail to find an actor" in {
      val bankDao = new BankDao(new InMemoryClientDao, new InMemoryAccountDao(), new InMemoryTransactionDao)

      val replyProbe  = createTestProbe[ClientActor.Response]
      val testClient1 = spawn(ClientActor(ClientId(), bankDao, 100))

      testClient1 ! ClientActor.Get(replyProbe.ref)

      replyProbe.expectNoMessage()
    }
  }
}
