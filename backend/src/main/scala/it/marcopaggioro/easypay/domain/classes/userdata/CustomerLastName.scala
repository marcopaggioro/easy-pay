package it.marcopaggioro.easypay.domain.classes.userdata

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.{Decoder, Encoder}
import it.marcopaggioro.easypay.domain.classes.Validable

case class CustomerLastName private(value: String) extends Validable[CustomerLastName] {

  private lazy val maxLength: Int = 50

  override def validate(): ValidatedNel[String, CustomerLastName] =
    condNel(value.nonEmpty, this, "Last name must not be empty")
      .andThen(_ => condNel(value.length <= maxLength, this, s"Last name must be a maximum of $maxLength characters long"))

}

object CustomerLastName {

  private def capitalizeSingleWords(value: String): String = value.split("\\s+").map(_.capitalize).mkString(" ")

  def apply(value: String): CustomerLastName = new CustomerLastName(capitalizeSingleWords(value.trim.toLowerCase))

  implicit val CustomerLastNameEncoder: Encoder[CustomerLastName] = Encoder.encodeString.contramap(_.value)
  implicit val CustomerLastNameDecoder: Decoder[CustomerLastName] = Decoder.decodeString.map(CustomerLastName.apply)

}
