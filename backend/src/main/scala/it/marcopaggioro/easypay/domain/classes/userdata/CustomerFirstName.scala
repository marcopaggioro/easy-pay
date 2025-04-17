package it.marcopaggioro.easypay.domain.classes.userdata

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.{Decoder, Encoder}
import it.marcopaggioro.easypay.domain.classes.Validable
import it.marcopaggioro.easypay.domain.classes.userdata.CustomerFirstName.maxLength
import it.marcopaggioro.easypay.utilities.ValidationUtilities.noNumbersRegex

case class CustomerFirstName private (value: String) extends Validable[CustomerFirstName] {

  override def validate(): ValidatedNel[String, CustomerFirstName] =
    condNel(value.nonEmpty, this, "Il nome non può essere vuoto")
      .andThen(_ => condNel(value.length <= maxLength, this, s"Il nome può essere lungo al massimo $maxLength caratteri"))
      .andThen(_ => condNel(noNumbersRegex.matches(value), this, "Il nome non può contenere numeri"))
}

object CustomerFirstName {
  lazy val maxLength: Int = 50

  private def capitalizeSingleWords(value: String): String = value.split("\\s+").map(_.capitalize).mkString(" ")

  def apply(value: String): CustomerFirstName = new CustomerFirstName(capitalizeSingleWords(value.trim.toLowerCase))

  implicit val CustomerFirstNameEncoder: Encoder[CustomerFirstName] = Encoder.encodeString.contramap(_.value)
  implicit val CustomerFirstNameDecoder: Decoder[CustomerFirstName] = Decoder.decodeString.map(CustomerFirstName.apply)

}
