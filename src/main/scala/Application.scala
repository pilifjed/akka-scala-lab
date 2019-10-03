import akka.actor.{ActorRef, ActorSystem}

import scala.io.StdIn

object Application extends App {
  import OrderManager._
  val system: ActorSystem = ActorSystem("esklep")
  val orderManager: ActorRef = system.actorOf(OrderManager.props, "orderManager")
  val input = StdIn
  while(true){
    val line :Array[String] = input.readLine().trim.split(" ")
    line(0) match {
      case "select" => orderManager ! SelectDeliveryAndPaymentMethod(line(1), line(2))
      case "add" => orderManager ! AddItem(line(1))
      case "rem" => orderManager ! RemoveItem(line(1))
      case "buy" => orderManager ! Buy
      case "pay" => orderManager ! Pay
      case _ => println("Unknown command")
    }
  }
}
