package it.marcopaggioro.easypay.routes.payloads

import cats.data.ValidatedNel
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import it.marcopaggioro.easypay.domain.classes.Validable
import it.marcopaggioro.easypay.domain.classes.userdata.{Email, EncryptedPassword}

case class LoginPayload(email: Email, encryptedPassword: EncryptedPassword) extends Validable[LoginPayload] {
  override def validate(): ValidatedNel[String, LoginPayload] = email
    .validate()
    .andThen(_ => encryptedPassword.validate())
    .map(_ => this)
}

object LoginPayload {

  implicit val LoginPayloadDecoder: Decoder[LoginPayload] = deriveDecoder[LoginPayload]

}
