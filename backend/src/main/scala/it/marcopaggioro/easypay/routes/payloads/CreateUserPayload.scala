package it.marcopaggioro.easypay.routes.payloads

import cats.data.ValidatedNel
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import it.marcopaggioro.easypay.domain.classes.{Email, UserData, Validable}


case class CreateUserPayload(userData: UserData) extends Validable[CreateUserPayload] {
  override def validate(): ValidatedNel[String, CreateUserPayload] = userData.validate().map(_ => this)
}

object CreateUserPayload {

  implicit val CreateUserPayloadDecoder: Decoder[CreateUserPayload] = deriveDecoder[CreateUserPayload]

}
