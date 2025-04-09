package it.marcopaggioro.easypay.domain.classes

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.{Decoder, Encoder}

import scala.util.matching.compat.Regex

case class Email private (value: String) extends Validable[Email] {

  private lazy val emailRegex: Regex = new Regex("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b")

  // https://www.regular-expressions.info/email.html
  override def validate(): ValidatedNel[String, Email] =
    condNel(value.nonEmpty, this, "Email can not be empty")
      .andThen(_ => condNel(emailRegex.matches(value.toUpperCase), this, "Invalid email"))

}

object Email {

  def apply(value: String) = new Email(value.trim.toLowerCase)

  implicit val EmailDecoder: Decoder[Email] = Decoder.decodeString.map(Email.apply)
  implicit val EmailEncoder: Encoder[Email] = Encoder.encodeString.contramap(_.value)

}
