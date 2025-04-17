package it.marcopaggioro.easypay.database.userpaymentcards

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.userdata.CustomerFullName

case class UserPaymentCardRecord(
    customerId: CustomerId,
    cardId: Int,
    fullName: CustomerFullName,
    cardNumber: String,
    expiration: String
)

object UserPaymentCardRecord {

  implicit val UserPaymentCardRecordEncoder: Encoder[UserPaymentCardRecord] = Encoder.instance { userPaymentCardRecord =>
    Json.obj(
      "cardId" -> userPaymentCardRecord.cardId.asJson,
      "fullName" -> userPaymentCardRecord.fullName.asJson,
      "cardNumber" -> userPaymentCardRecord.cardNumber.asJson,
      "expiration" -> userPaymentCardRecord.expiration.asJson
    )
  }

}
