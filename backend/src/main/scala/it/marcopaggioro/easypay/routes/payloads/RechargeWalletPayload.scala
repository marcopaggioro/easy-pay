package it.marcopaggioro.easypay.routes.payloads

import cats.data.ValidatedNel
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import it.marcopaggioro.easypay.domain.classes.{Money, Validable}
import it.marcopaggioro.easypay.utilities.ValidationUtilities.{validateAmount, validateCardId}

case class RechargeWalletPayload(cardId: Int, amount: Money) extends Validable[RechargeWalletPayload] {

  override def validate(): ValidatedNel[String, RechargeWalletPayload] = validateCardId(cardId)
    .andThen(_ => validateAmount(amount))
    .map(_ => this)

}

object RechargeWalletPayload {

  implicit val RechargeWalletPayloadDecoder: Decoder[RechargeWalletPayload] = deriveDecoder[RechargeWalletPayload]

}
