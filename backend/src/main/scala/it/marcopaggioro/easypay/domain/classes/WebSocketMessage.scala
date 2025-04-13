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
      Json.obj("type" -> "customer_registered".asJson)

    case WalletUpdated =>
      Json.obj("type" -> "wallet_updated".asJson)

    case ScheduledOperationsUpdated =>
      Json.obj("type" -> "scheduled_operations_updated".asJson)

    case UserDataUpdated =>
      Json.obj("type" -> "user_data_updated".asJson)
  }

}
