package it.marcopaggioro.easypay.domain.classes.userdata

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.{Decoder, Encoder}
import it.marcopaggioro.easypay.domain.classes.Validable
import it.marcopaggioro.easypay.domain.classes.userdata.CustomerLastName.maxLength

case class CustomerLastName private (value: String) extends Validable[CustomerLastName] {

  override def validate(): ValidatedNel[String, CustomerLastName] =
    condNel(value.nonEmpty, this, "Il cognome non può essere vuoto")
      .andThen(_ => condNel(value.length <= maxLength, this, s"Il cognome può essere lungo al massimo $maxLength caratteri"))

}

object CustomerLastName {
  lazy val maxLength: Int = 50

  private def capitalizeSingleWords(value: String): String = value.split("\\s+").map(_.capitalize).mkString(" ")

  def apply(value: String): CustomerLastName = new CustomerLastName(capitalizeSingleWords(value.trim.toLowerCase))

  implicit val CustomerLastNameEncoder: Encoder[CustomerLastName] = Encoder.encodeString.contramap(_.value)
  implicit val CustomerLastNameDecoder: Decoder[CustomerLastName] = Decoder.decodeString.map(CustomerLastName.apply)

}
