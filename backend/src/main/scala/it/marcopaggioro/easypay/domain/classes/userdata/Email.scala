package it.marcopaggioro.easypay.domain.classes.userdata

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.{Decoder, Encoder}
import it.marcopaggioro.easypay.domain.classes.Validable
import it.marcopaggioro.easypay.domain.classes.userdata.Email.maxLength

import scala.util.matching.compat.Regex

case class Email private (value: String) extends Validable[Email] {

  // https://www.regular-expressions.info/email.html
  private lazy val emailRegex: Regex = new Regex("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b")

  override def validate(): ValidatedNel[String, Email] =
    condNel(value.nonEmpty, this, "L'email non può essere vuota")
      .andThen(_ => condNel(value.length <= maxLength, this, s"L'email può essere lunga al massimo $maxLength caratteri"))
      .andThen(_ => condNel(emailRegex.matches(value.toUpperCase), this, "Email invalida"))

}

object Email {

  lazy val maxLength: Int = 254

  def apply(value: String) = new Email(value.trim.toLowerCase)

  implicit val EmailDecoder: Decoder[Email] = Decoder.decodeString.map(Email.apply)
  implicit val EmailEncoder: Encoder[Email] = Encoder.encodeString.contramap(_.value)

}
