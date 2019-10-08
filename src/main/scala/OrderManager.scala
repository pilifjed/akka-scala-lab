import akka.actor.{Actor, ActorRef, FSM, Props}
import akka.pattern.ask

import scala.concurrent.duration._
import akka.event.Logging
import akka.util.Timeout

import scala.concurrent.Await

object OrderManager {
  def props : Props = Props[OrderManager]

  sealed trait State
  case object Uninitialized extends State
  case object Open          extends State
  case object InCheckout    extends State
  case object InPayment     extends State
  case object Finished      extends State

  sealed trait Command
  case class AddItem(item: String)    extends Command
  case class RemoveItem(item: String) extends Command
  case class SelectDeliveryAndPaymentMethod(delivery: String, payment: String) extends Command
  case object Buy extends Command
  case object CancelBuy extends Command
  case object Pay extends Command

  sealed trait Ack
  case object Done  extends Ack

  sealed trait Data
  case object Empty extends Data
  case class CartData(master: ActorRef, cartRef: ActorRef) extends Data
  case class InCheckoutData(master: ActorRef, checkoutRef: ActorRef)  extends Data
  case class InPaymentData(master: ActorRef, paymentRef: ActorRef)    extends Data
}

class OrderManager extends Actor with FSM[OrderManager.State, OrderManager.Data] {
  import OrderManager._
  //override val log = Logging(context.system, this)

  startWith(Uninitialized, Empty)

  when(Uninitialized){
    case Event(command :AddItem, Empty) =>
      val cartRef = context.actorOf(Props[CartManager])
      cartRef ! command
      goto(Open).using(CartData(sender(),cartRef))
  }

  when(Open){
    case Event(command : AddItem, CartData(_,cartRef)) =>
      cartRef ! command
      stay().using(CartData(sender(),cartRef))
    case Event(command : RemoveItem, CartData(_,cartRef)) =>
      cartRef ! command
      stay().using(CartData(sender(),cartRef))
    case Event(Buy, CartData(_,cartRef)) =>
      cartRef ! Buy
      stay().using(CartData(sender(),cartRef))
    case Event(CartManager.CheckOutStarted(ref), CartData(master, _)) =>
      log.debug("Checkout Started")
      goto(InCheckout).using(InCheckoutData(master, ref))
    case Event(CartManager.CartEmpty, _) =>
      log.debug("Cart Empty")
      goto(Uninitialized).using(Empty)
    case Event(Done, CartData(master, _)) =>
      log.debug("Done")
      master ! Done
      stay()

  }

  when(InCheckout){
    case Event(SelectDeliveryAndPaymentMethod(delivery, payment), InCheckoutData(_ ,checkoutRef)) =>
      implicit val timeout : akka.util.Timeout = Timeout(5 seconds)
      val future = checkoutRef ? Checkout.SelectDelivery(delivery)
      Await.result(future, timeout.duration)
      checkoutRef ! Checkout.SelectPayment(payment)
      stay().using(InCheckoutData(sender(), checkoutRef))
    case Event(Checkout.PaymentServiceStarted(paymentRef), InCheckoutData(master, _)) =>
      log.debug("Payment started")
      goto(InPayment).using(InPaymentData(master, paymentRef))
  }

  when(InPayment){
    case Event(Pay, InPaymentData(_, paymentRef)) =>
      paymentRef ! Pay
      stay().using(InPaymentData(sender(), paymentRef))
    case Event(Payment.PaymentConfirmed, _) =>
      log.debug("Payment confirmed")
      goto(Finished).using(Empty)
  }

  when(Finished, stateTimeout = 10 seconds){
    case Event(CartManager.CartEmpty,_) =>
      stay()
    case Event(StateTimeout, _) =>
      goto(Uninitialized).using(Empty)
  }

  onTransition {
    case Open -> InCheckout =>
      stateData match {
        case CartData(master, _) => master ! Done
      }
    case InCheckout -> InPayment =>
      stateData match {
        case InCheckoutData(master, _) => master ! Done
      }
    case InPayment -> Finished =>
      stateData match {
        case InPaymentData(master, _) => master ! Done
      }
  }
}
