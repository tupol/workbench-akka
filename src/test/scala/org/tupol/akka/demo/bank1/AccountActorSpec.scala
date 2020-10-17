package org.tupol.akka.demo.bank1

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.{ after => aafter }
import org.scalatest.wordspec.AnyWordSpecLike
import org.tupol.akka.demo.bank1.domain.{
  Account,
  AccountId,
  BankEngine,
  InMemoryAccountDao,
  InMemoryTransactionDao,
  Transaction
}
import org.tupol.akka.demo.test1.DataAccess

import scala.concurrent.Future
import scala.concurrent.duration._

class AccountActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "AccountActor" must {
    "run" in {
      implicit val ec = scala.concurrent.ExecutionContext.global

      val bankEngine = new BankEngine(new InMemoryAccountDao, new InMemoryTransactionDao)

      val bac1 = bankEngine.accountDao.create().futureValue
      val bac2 = bankEngine.accountDao.create().futureValue

      println(s"ACCOUNT 1 = $bac1")
      println(s"ACCOUNT 2 = $bac2")

      val accReplyProbe  = createTestProbe[Account]()
      val accReplyProbe2 = createTestProbe[Account]()
      val testedActor1   = spawn(AccountActor.apply(bac1.id, bankEngine))
      val testedActor2   = spawn(AccountActor.apply(bac2.id, bankEngine))

      val trnReplyProbe = createTestProbe[Transaction]()
      val transaction1  = bankEngine.transactionDao.create(AccountId(), bac1.id, 100).futureValue

      testedActor1 ! AccountActor.Deactivate(accReplyProbe.ref)
      testedActor1 ! AccountActor.Activate(accReplyProbe.ref)
      testedActor1 ! AccountActor.Receive(transaction1, trnReplyProbe.ref)
      testedActor1 ! AccountActor.Get(accReplyProbe.ref)
      testedActor2 ! AccountActor.Get(accReplyProbe2.ref)
      testedActor1 ! AccountActor.Pay(bac2.id, 10, trnReplyProbe.ref)
      testedActor1 ! AccountActor.Pay(bac2.id, 20, trnReplyProbe.ref)
      testedActor1 ! AccountActor.Pay(bac2.id, 30, trnReplyProbe.ref)
      testedActor1 ! AccountActor.Get(accReplyProbe.ref)
      testedActor2 ! AccountActor.Get(accReplyProbe2.ref)

      println(accReplyProbe.receiveMessage())
      println(accReplyProbe.receiveMessage())
      println(trnReplyProbe.receiveMessage())
      println(accReplyProbe.receiveMessage())
      println(accReplyProbe2.receiveMessage())
      println(trnReplyProbe.receiveMessage())
      println(trnReplyProbe.receiveMessage())
      println(trnReplyProbe.receiveMessage())
      println(accReplyProbe.receiveMessage())
      println(accReplyProbe2.receiveMessage())

    }
  }

}
