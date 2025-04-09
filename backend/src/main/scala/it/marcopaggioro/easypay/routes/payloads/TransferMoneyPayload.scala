package it.marcopaggioro.easypay.routes.payloads

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import it.marcopaggioro.easypay.domain.classes.{Email, Money, Validable}
import it.marcopaggioro.easypay.utilities.ValidationUtilities.validatePositiveAmount

case class TransferMoneyPayload(recipientEmail: Email, amount: Money) extends Validable[TransferMoneyPayload] {
  override def validate(): ValidatedNel[String, TransferMoneyPayload] = recipientEmail
    .validate()
    .andThen(_ => validatePositiveAmount(amount))
    .map(_ => this)
}

object TransferMoneyPayload {

  implicit val TransferMoneyPayloadDecoder: Decoder[TransferMoneyPayload] = deriveDecoder[TransferMoneyPayload]

}
