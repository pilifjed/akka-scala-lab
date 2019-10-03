import akka.actor.{Actor, ActorRef, FSM, Props}

import scala.concurrent.duration._

object Cart{
  def props :Props = Props[Cart]
  case class CheckOutStarted(checkout : ActorRef)
  case object CartEmpty

  trait State
  case object Empty extends State
  case object NonEmpty extends State
  case object InCheckout extends State

  trait Data
  case class ItemsInCart(items :Set[String], count :Int) extends Data
}

class Cart extends Actor with FSM[Cart.State, Cart.Data]{
  import Cart._

  startWith(Empty, ItemsInCart(Set.empty,0))

  when(Empty){
    case Event(OrderManager.AddItem(item), ItemsInCart(items,count)) =>
      sender() ! OrderManager.Done
      goto(NonEmpty).using(ItemsInCart(items + item, count + 1))
  }

  when(NonEmpty, stateTimeout = 30 second){
    case Event(OrderManager.AddItem(item), ItemsInCart(items,count)) =>
      sender() ! OrderManager.Done
      stay().using(ItemsInCart(items + item, count + 1))
    case Event(OrderManager.RemoveItem(item), ItemsInCart(items, count)) =>
      if(items.contains(item)){
        if(count>1){
          sender() ! OrderManager.Done
          stay().using(ItemsInCart(items - item, count - 1))
        }
        else{
          sender() ! OrderManager.Done
          goto(Empty).using(ItemsInCart(Set.empty, 0))
        }
      } else {
        sender() ! OrderManager.Done
        stay().replying()
      }
    case Event(OrderManager.Buy, items) =>
      val checkout = context.actorOf(Props[Checkout])
      sender() ! CheckOutStarted(checkout)
      goto(InCheckout).using(items)
    case Event(StateTimeout,_) =>
      context.parent ! CartEmpty
      goto(Empty).using(ItemsInCart(Set.empty, 0))
  }

  when(InCheckout){
    case Event(Checkout.CheckOutClosed, _) =>
      context.stop(sender())
      context.parent ! CartEmpty
      context.stop(self)
      stay()
  }
}