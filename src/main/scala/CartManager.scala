import akka.actor.{Actor, ActorRef, FSM, Props}
import akka.persistence.fsm._
import akka.persistence.fsm.PersistentFSM.{Event, FSMState, StateChangeEvent}

import scala.concurrent.duration._
import scala.reflect._

object CartManager{
  def props :Props = Props[CartManager]
  case class CheckOutStarted(checkout : ActorRef)
  case object CartEmpty

  trait State extends FSMState
  case object Empty extends State {
    override def identifier: String = "Empty"
  }
  case object NonEmpty extends State {
    override def identifier: String = "NonEmpty"
  }
  case object InCheckout extends State {
    override def identifier: String = "InCheckout"
  }

  sealed trait DomainEvent
  case class AddItemEvent(item : String) extends DomainEvent
  case class RemoveItemEvent(item : String) extends DomainEvent
  case object NoChangeEvent extends DomainEvent
  case object ResetEvent extends DomainEvent

  trait Data
  case class Cart() extends Data {
    private var items: Set[String] = Set.empty
    private var count : Int = 0

    def addItem(item : String) : Cart = {
      this.items = this.items + item
      this.count = this.items.size
      this
    }

    def removeItem(item : String) : Cart = {
      this.items = this.items - item
      this.count = this.items.size
      this
    }
    def getCount : Int = {
      this.count
    }
  }
}

class CartManager extends Actor with PersistentFSM[CartManager.State, CartManager.Cart, CartManager.DomainEvent]{
  import CartManager._

  override def persistenceId = "cart-manager-persistence-id"

  override def domainEventClassTag: ClassTag[DomainEvent] = classTag[DomainEvent]

  override def applyEvent(domainEvent: DomainEvent, currentData: Cart): Cart = {
    domainEvent match {
      case AddItemEvent(item) => currentData.addItem(item)
      case RemoveItemEvent(item) => currentData.removeItem(item)
      case NoChangeEvent => currentData
      case ResetEvent => Cart()
    }
  }


  startWith(Empty, Cart())

  when(Empty){
    case Event(OrderManager.AddItem(item), _) =>
      goto(NonEmpty).applying(AddItemEvent(item)).andThen(_ => sender() ! OrderManager.Done)
  }

  when(NonEmpty, stateTimeout = 30 second){
    case Event(OrderManager.AddItem(item), _) =>
      stay().applying(AddItemEvent(item)).replying(OrderManager.Done)
    case Event(OrderManager.RemoveItem(item), cart :Cart) =>
      if(cart.getCount > 1){
          stay().applying(RemoveItemEvent(item)).replying(OrderManager.Done)
      }
      else{
          goto(Empty).applying(RemoveItemEvent(item)).replying(OrderManager.Done)
      }
    case Event(OrderManager.Buy, _) =>
      val checkout = context.actorOf(Props[Checkout])
      sender() ! CheckOutStarted(checkout)
      goto(InCheckout).applying(NoChangeEvent)
    case Event(StateTimeout,_) =>
      context.parent ! CartEmpty
      goto(Empty).applying(ResetEvent)
  }

  when(InCheckout){
    case Event(Checkout.CheckOutClosed, _) =>
      context.stop(sender())
      context.parent ! CartEmpty
      context.stop(self)
      stay()
  }
}