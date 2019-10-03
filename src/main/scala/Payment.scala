import akka.actor.{Actor, Props}

object Payment {
  def props :Props = Props[Payment]

  case object PaymentConfirmed
  case object PaymentReceived

}

class Payment extends Actor{
  import Payment._

  override def receive: Receive = {
    case OrderManager.Pay =>
      sender() ! PaymentConfirmed
      context.parent ! PaymentReceived
  }
}
