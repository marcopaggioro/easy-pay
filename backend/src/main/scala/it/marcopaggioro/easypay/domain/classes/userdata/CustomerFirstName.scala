package it.marcopaggioro.easypay.domain.classes.userdata

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.{Decoder, Encoder}
import it.marcopaggioro.easypay.domain.classes.Validable

case class CustomerFirstName private (value: String) extends Validable[CustomerFirstName] {

  private lazy val maxLength: Int = 50
  override def validate(): ValidatedNel[String, CustomerFirstName] =
    condNel(value.nonEmpty, this, "First name can not be empty")
      .andThen(_ => condNel(value.length <= maxLength, this, s"First name must be a maximum of $maxLength characters long"))

}

object CustomerFirstName {

  private def capitalizeSingleWords(value: String): String = value.split("\\s+").map(_.capitalize).mkString(" ")

  def apply(value: String): CustomerFirstName = new CustomerFirstName(capitalizeSingleWords(value.trim.toLowerCase))

  implicit val CustomerFirstNameEncoder: Encoder[CustomerFirstName] = Encoder.encodeString.contramap(_.value)
  implicit val CustomerFirstNameDecoder: Decoder[CustomerFirstName] = Decoder.decodeString.map(CustomerFirstName.apply)

}
