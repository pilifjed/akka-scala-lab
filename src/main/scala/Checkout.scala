import akka.actor.{Actor, ActorRef, FSM, PoisonPill, Props}

object Checkout{
  def props :Props = Props[Checkout]

  case class SelectDelivery(delivery: String)
  case class SelectPayment(payment: String)
  case class PaymentServiceStarted(paymentRef: ActorRef)
  case object CheckOutClosed

  trait State
  case object SelectingDelivery extends State
  case object SelectingPayment extends State
  case object ProcessingPayment extends State

  trait Data
  case class CheckoutInformation(delivery: String, payment: String) extends Data
}

class Checkout extends Actor with FSM[Checkout.State, Checkout.Data]{
  import Checkout._

  startWith(SelectingDelivery, CheckoutInformation("",""))

  when(SelectingDelivery){
    case Event(SelectDelivery(delivery), _) =>
      sender() ! OrderManager.Done
      goto(SelectingPayment).using(CheckoutInformation(delivery,""))
  }
  when(SelectingPayment){
    case Event(SelectPayment(payment), data : CheckoutInformation) =>
      val paymentRef = context.actorOf(Props[Payment])
      sender() ! PaymentServiceStarted(paymentRef)
      goto(ProcessingPayment).using(CheckoutInformation(data.delivery, payment))
  }

  when(ProcessingPayment){
    case Event(Payment.PaymentReceived, _) =>
      context.stop(sender())
      context.parent ! CheckOutClosed
      stay()
  }
}
