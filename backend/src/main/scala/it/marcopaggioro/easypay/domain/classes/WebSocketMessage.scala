package it.marcopaggioro.easypay.domain.classes

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

sealed trait WebSocketMessage

object WebSocketMessage {

  case object CustomerRegistered extends WebSocketMessage
  case object WalletUpdated extends WebSocketMessage
  case object ScheduledOperationsUpdated extends WebSocketMessage
  case object UserDataUpdated extends WebSocketMessage

  implicit val WebSocketMessageEncoder: Encoder[WebSocketMessage] = Encoder.instance {
    case CustomerRegistered =>
      Json.obj("event" -> "customer_registered".asJson)

    case WalletUpdated =>
      Json.obj("event" -> "wallet_updated".asJson)

    case ScheduledOperationsUpdated =>
      Json.obj("event" -> "scheduled_operations_updated".asJson)

    case UserDataUpdated =>
      Json.obj("event" -> "user_data_updated".asJson)
  }

}
