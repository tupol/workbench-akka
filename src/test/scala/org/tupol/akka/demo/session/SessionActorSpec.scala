package org.tupol.akka.demo.session

import akka.actor.testkit.typed.scaladsl.{ ManualTime, ScalaTestWithActorTestKit }
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class SessionActorSpec extends ScalaTestWithActorTestKit(ManualTime.config) with AnyWordSpecLike {

  override implicit val patience: PatienceConfig =
    PatienceConfig(10.seconds, Span(100, org.scalatest.time.Millis))

  val manualTime: ManualTime = ManualTime()

  "SessionActor" should {

    val sessionId = SessionId()

    "idle when no action" in {
      val sessionProbe = createTestProbe[Session]
      val testSession  = spawn(SessionActor(sessionId, 10.millis, 100.millis))
      manualTime.expectNoMessageFor(11.millis)
      testSession ! SessionActor.Get(sessionProbe.ref)
      sessionProbe.expectNoMessage()
    }

    "idle after some action" in {
      val sessionProbe = createTestProbe[Session]
      val testSession  = spawn(SessionActor(sessionId, 10.millis, 100.millis))
      manualTime.expectNoMessageFor(5.millis)
      testSession ! SessionActor.Get(sessionProbe.ref)
      sessionProbe.expectMessageType[Session]
      manualTime.timePasses(11.millis)
      testSession ! SessionActor.Get(sessionProbe.ref)
      sessionProbe.expectNoMessage()
    }

    "expire" in {
      val sessionProbe = createTestProbe[Session]
      val testSession  = spawn(SessionActor(sessionId, 10.millis, 15.millis))
      manualTime.expectNoMessageFor(9.millis)
      testSession ! SessionActor.Get(sessionProbe.ref)
      sessionProbe.expectMessageType[Session]
      manualTime.timePasses(11.millis)
      manualTime.expectNoMessageFor(9.millis)
      sessionProbe.expectNoMessage()
    }

    "terminate" in {
      val sessionProbe = createTestProbe[Session]
      val testSession  = spawn(SessionActor(sessionId, 10.millis, 15.millis))
      testSession ! SessionActor.Terminate
      testSession ! SessionActor.Get(sessionProbe.ref)
      sessionProbe.expectNoMessage()
    }

    "change state" in {
      val sessionProbe = createTestProbe[Session]
      val testSession  = spawn(SessionActor(sessionId, 1.seconds, 4.seconds))

      testSession ! SessionActor.Put("a", "b")
      testSession ! SessionActor.Get(sessionProbe.ref)
      sessionProbe.receiveMessage().state shouldBe Map("a" -> "b")
    }
  }

}
