package it.marcopaggioro.easypay.routes.payloads

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import it.marcopaggioro.easypay.domain.classes.Aliases.EncryptedPassword
import it.marcopaggioro.easypay.domain.classes.{Email, Validable}
import it.marcopaggioro.easypay.utilities.ValidationUtilities.validateEncryptedPassword

case class LoginPayload(email: Email, encryptedPassword: EncryptedPassword) extends Validable[LoginPayload] {
  override def validate(): ValidatedNel[String, LoginPayload] = email
    .validate()
    .andThen(_ => validateEncryptedPassword(encryptedPassword))
    .map(_ => this)
}

object LoginPayload {

  implicit val LoginPayloadDecoder: Decoder[LoginPayload] = deriveDecoder[LoginPayload]

}
