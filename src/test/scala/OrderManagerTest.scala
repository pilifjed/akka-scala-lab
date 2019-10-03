import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{TestFSMRef, TestKit}
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


class OrderManagerTest
  extends TestKit(ActorSystem("OrderManagerTest"))
    with WordSpecLike
    with BeforeAndAfterAll
    with ScalaFutures
    with Matchers {

  implicit val timeout: Timeout = 1.second

  "An order manager" must {
    "supervise whole order process" in {
      import OrderManager._

      def sendMessageAndValidateState(orderManager: TestFSMRef[State, Data, OrderManager],
                                      message: OrderManager.Command,
                                      expectedState: OrderManager.State): Unit = {
        (orderManager ? message).mapTo[OrderManager.Ack].futureValue shouldBe Done
        orderManager.stateName shouldBe expectedState
      }

      val orderManager = TestFSMRef[State, Data, OrderManager](new OrderManager())
      orderManager.stateName shouldBe Uninitialized
      sendMessageAndValidateState(orderManager, AddItem("rollerblades"), Open)
      sendMessageAndValidateState(orderManager, Buy, InCheckout)
      sendMessageAndValidateState(orderManager, SelectDeliveryAndPaymentMethod("paypal", "inpost"), InPayment)
      sendMessageAndValidateState(orderManager, Pay, Finished)
    }
  }

}