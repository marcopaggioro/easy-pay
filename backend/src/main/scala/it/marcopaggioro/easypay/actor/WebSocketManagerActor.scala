package it.marcopaggioro.easypay.actor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.ws.TextMessage
import io.circe.syntax.EncoderOps
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.WebSocketMessage

object WebSocketManagerActor {

  lazy val Name: String = "web-socket-manager-actor"

  sealed trait WebSocketManagerActorCommand
  final case class Register(customerId: CustomerId, actorRef: ActorRef[TextMessage.Strict]) extends WebSocketManagerActorCommand
  final case class Unregister(customerId: CustomerId) extends WebSocketManagerActorCommand
  final case class SendMessage(customerId: CustomerId, message: WebSocketMessage) extends WebSocketManagerActorCommand

  // TODO doppia mappa con email per notificare l'email registrata?
  private def withClients(clients: Map[CustomerId, ActorRef[TextMessage.Strict]]): Behavior[WebSocketManagerActorCommand] =
    Behaviors.receive[WebSocketManagerActorCommand] { case (context, command) =>
      command match {
        case Register(customerId, actorRef) =>
          context.log.info(s"Registering WS for $customerId")
          withClients(clients.updated(customerId, actorRef))

        case Unregister(customerId) =>
          context.log.info(s"Unregistering WS for $customerId")
          withClients(clients.removed(customerId))

        case SendMessage(customerId, message) =>
          clients.get(customerId) match {
            case Some(actorRef) =>
              context.log.debug(s"Sending WS message to $customerId: $message")
              actorRef ! TextMessage.Strict(message.asJson.noSpaces)
              Behaviors.same

            case None =>
              context.log.debug(s"WS client not found for $customerId")
              Behaviors.same
          }
      }
    }

  def apply(): Behavior[WebSocketManagerActorCommand] = withClients(Map.empty)

}
