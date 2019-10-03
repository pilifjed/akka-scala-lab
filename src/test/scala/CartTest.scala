import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{TestFSMRef, TestKit}
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._


class CartTest
  extends TestKit(ActorSystem("CartTest"))
    with WordSpecLike
    with BeforeAndAfterAll
    with ScalaFutures
    with Matchers {

  implicit val timeout: Timeout = 1.second

  "A cart" must {
    "supervise whole item management process" in {
      import Cart._

      val cart = TestFSMRef[State, Data, Cart](new Cart())

      cart.stateName shouldBe Empty

      // add remove items tests

      val addItem1Future = cart ? OrderManager.AddItem("rollerblades")
      val addItem1Res = Await.result(addItem1Future, timeout.duration)
      addItem1Res shouldBe OrderManager.Done

      assert(cart.stateData == ItemsInCart(Set("rollerblades"), 1))
      cart.stateName shouldBe NonEmpty

      val addItem2Future = cart ? OrderManager.AddItem("sleds")
      val addItem2Res = Await.result(addItem2Future, timeout.duration)
      addItem2Res shouldBe OrderManager.Done

      assert(cart.stateData == ItemsInCart(Set("rollerblades", "sleds"), 2))
      cart.stateName shouldBe NonEmpty

      val remItem1Future = cart ? OrderManager.RemoveItem("sleds")
      val remItem1Res = Await.result(remItem1Future, timeout.duration)
      remItem1Res shouldBe OrderManager.Done

      assert(cart.stateData == ItemsInCart(Set("rollerblades"), 1))
      cart.stateName shouldBe NonEmpty

      val remItem2Future = cart ? OrderManager.RemoveItem("rollerblades")
      val remItem2Res = Await.result(remItem2Future, timeout.duration)
      remItem2Res shouldBe OrderManager.Done

      assert(cart.stateData == ItemsInCart(Set.empty, 0))
      cart.stateName shouldBe Empty

      //Buying tests

      val addItem3Future = cart ? OrderManager.AddItem("rollerblades")
      val addItem3Res = Await.result(addItem3Future, timeout.duration)
      addItem3Res shouldBe OrderManager.Done

      assert(cart.stateData == ItemsInCart(Set("rollerblades"), 1))
      cart.stateName shouldBe NonEmpty

      val buyFuture = cart ? OrderManager.Buy
      val buyRes = Await.result(buyFuture, timeout.duration)
      assert(buyRes.isInstanceOf[Cart.CheckOutStarted])
      cart.stateName shouldBe InCheckout

//      buyRes match {
//        case Cart.CheckOutStarted(checkoutRef) => {
//          val selectdeliveryFuture = checkoutRef ? Checkout.SelectDelivery("delivery")
//          val selectdeliveryRes = Await.result(selectdeliveryFuture, timeout.duration)
//          selectdeliveryRes shouldBe OrderManager.Done
//
//        }
//      }
    }
  }

}