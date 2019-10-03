import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{TestFSMRef, TestKit, TestProbe}
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._


class CheckoutTest
  extends TestKit(ActorSystem("CartTest"))
    with WordSpecLike
    with BeforeAndAfterAll
    with ScalaFutures
    with Matchers {

  implicit val timeout: Timeout = 1.second

  "A checkout" must {
    "supervise delivary and payment selection process and send CheckOutClosed to parent" in {
      import Checkout._
      val cart = TestProbe()
      val checkout = cart.childActorOf(Props(new Checkout))

      val selectDeliveryFuture = checkout ? Checkout.SelectDelivery("delivery")
      val selectDeliveryRes = Await.result(selectDeliveryFuture, timeout.duration)
      selectDeliveryRes shouldBe OrderManager.Done

      val selectPaymentFuture = checkout ? Checkout.SelectPayment("payment")
      val selectPaymentRes = Await.result(selectPaymentFuture, timeout.duration)
      assert(selectPaymentRes.isInstanceOf[PaymentServiceStarted])

      checkout ! Payment.PaymentReceived
      cart.expectMsg(CheckOutClosed)
    }
  }

}