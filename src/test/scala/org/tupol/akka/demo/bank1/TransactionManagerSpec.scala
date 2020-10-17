package org.tupol.akka.demo.bank1

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import org.scalatest.wordspec.AnyWordSpecLike
import org.tupol.akka.demo.bank1.domain._

class TransactionManagerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "TransactionManager" must {
    "run" in {
      implicit val ec = scala.concurrent.ExecutionContext.global

      val bankEngine = new BankEngine(new InMemoryAccountDao, new InMemoryTransactionDao)

      val bac1 = bankEngine.accountDao.create().futureValue
      val bac2 = bankEngine.accountDao.create().futureValue

      println(s"ACCOUNT 1 = $bac1")
      println(s"ACCOUNT 2 = $bac2")

      val accReplyProbe                                = createTestProbe[Account]()
      val accReplyProbe2                               = createTestProbe[Account]()
      val testedActor1: ActorRef[AccountActor.Command] = spawn(AccountActor.apply(bac1.id, bankEngine))
      val testedActor2                                 = spawn(AccountActor.apply(bac2.id, bankEngine))
      val testedTransM                                 = spawn(TransactionManager.apply(bankEngine))

      val trnReplyProbe = createTestProbe[Transaction]()
      val transaction1  = bankEngine.transactionDao.create(AccountId(), bac1.id, 100).futureValue

      testedActor1 ! AccountActor.Receive(transaction1, trnReplyProbe.ref)
      testedActor1 ! AccountActor.Pay(bac2.id, 10, trnReplyProbe.ref)

      println("----------------------------")
      val tx1 = trnReplyProbe.receiveMessage()
      println(tx1)
      println("----------------------------")
      val tx2 = trnReplyProbe.receiveMessage()
      println(tx2)
      testedTransM ! TransactionManager.Execute(tx2, trnReplyProbe.ref)
      println("----------------------------")
      val tx3 = trnReplyProbe.receiveMessage()
      println(tx3)
      println("----------------------------")

      testedActor1 ! AccountActor.Get(accReplyProbe.ref)
      testedActor2 ! AccountActor.Get(accReplyProbe2.ref)
      println(accReplyProbe.receiveMessage())
      println(accReplyProbe2.receiveMessage())

    }
  }

}
