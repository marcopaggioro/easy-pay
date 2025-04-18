package it.marcopaggioro.easypay.domain.classes.userdata

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.{Decoder, Encoder}
import it.marcopaggioro.easypay.domain.classes.Validable
import it.marcopaggioro.easypay.domain.classes.userdata.EncryptedPassword.maxLength

case class EncryptedPassword private (value: String) extends Validable[EncryptedPassword] {

  override def validate(): ValidatedNel[String, EncryptedPassword] =
    condNel(value.length == maxLength, this, "Crittografia della password invalida")

}

object EncryptedPassword {

  lazy val maxLength: Int = 128

  def apply(value: String): EncryptedPassword = new EncryptedPassword(value.trim)

  implicit val EncryptedPasswordEncoder: Encoder[EncryptedPassword] = Encoder.encodeString.contramap(_.value)
  implicit val EncryptedPasswordDecoder: Decoder[EncryptedPassword] = Decoder.decodeString.map(EncryptedPassword.apply)

}
