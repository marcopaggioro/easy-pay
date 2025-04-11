package it.marcopaggioro.easypay.domain.classes.userdata

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.{Decoder, Encoder}
import it.marcopaggioro.easypay.domain.classes.Validable

case class EncryptedPassword private (value: String) extends Validable[EncryptedPassword] {

  private lazy val encryptionLength: Int = 128
  override def validate(): ValidatedNel[String, EncryptedPassword] =
    condNel(value.length == encryptionLength, this, "Wrong password encryption")

}

object EncryptedPassword {

  def apply(value: String): EncryptedPassword = new EncryptedPassword(value.trim)

  implicit val EncryptedPasswordEncoder: Encoder[EncryptedPassword] = Encoder.encodeString.contramap(_.value)
  implicit val EncryptedPasswordDecoder: Decoder[EncryptedPassword] = Decoder.decodeString.map(EncryptedPassword.apply)

}
