package it.marcopaggioro.easypay.actor

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.ws.TextMessage
import io.circe.syntax.EncoderOps
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.WebSocketMessage

object WebSocketsManagerActor {

  type CustomersMapping = Map[CustomerId, Set[ActorRef[TextMessage.Strict]]]

  lazy val Name: String = "web-sockets-manager-actor"

  sealed trait WebSocketsManagerActorCommand
  final case class Register(customerId: CustomerId, actorRef: ActorRef[TextMessage.Strict]) extends WebSocketsManagerActorCommand
  final case class Unregister(customerId: CustomerId, actorRef: ActorRef[TextMessage.Strict])
      extends WebSocketsManagerActorCommand
  final case class SendMessage(customerId: CustomerId, message: WebSocketMessage) extends WebSocketsManagerActorCommand

  private def removeMapping(
      clients: CustomersMapping,
      customerId: CustomerId,
      actorRef: ActorRef[TextMessage.Strict]
  )(context: ActorContext[WebSocketsManagerActorCommand]): CustomersMapping = {
    context.log.info(s"Unregistering WS for $customerId ($actorRef)")

    clients.get(customerId) match {
      case Some(currentActorValues) =>
        val updatedActorRefs: Set[ActorRef[TextMessage.Strict]] = currentActorValues - actorRef
        if (updatedActorRefs.isEmpty) {
          clients.removed(customerId)
        } else {
          clients.updated(customerId, updatedActorRefs)
        }

      case None =>
        clients
    }
  }

  // TODO doppia mappa con email per notificare l'email registrata?
  private def withClients(clients: CustomersMapping): Behavior[WebSocketsManagerActorCommand] =
    Behaviors.receive[WebSocketsManagerActorCommand] { case (context, command) =>
      command match {
        case Register(customerId, actorRef) =>
          context.log.info(s"Registering WS for $customerId ($actorRef)")
          clients.get(customerId) match {
            case Some(currentActorValues) =>
              withClients(clients.updated(customerId, currentActorValues + actorRef))

            case None =>
              withClients(clients.updated(customerId, Set(actorRef)))
          }

        case Unregister(customerId, actorRef) =>
          withClients(removeMapping(clients, customerId, actorRef)(context))

        case SendMessage(customerId, message) =>
          clients.get(customerId) match {
            case Some(actorRefs) =>
              context.log.debug(s"Sending WS message to $customerId: $message")
              actorRefs.foreach { actorRef =>
                actorRef.tell(TextMessage.Strict(message.asJson.noSpaces))
              }
              Behaviors.same

            case None =>
              context.log.debug(s"WS client not found for $customerId")
              Behaviors.same
          }
      }
    }

  def apply(): Behavior[WebSocketsManagerActorCommand] = withClients(Map.empty)

}
