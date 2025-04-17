package it.marcopaggioro.easypay.domain.classes.userdata

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.{Decoder, Encoder}
import it.marcopaggioro.easypay.domain.classes.Validable

case class CustomerFullName private (value: String) extends Validable[CustomerFullName] {

  private lazy val maxLength: Int = CustomerFirstName.maxLength + CustomerLastName.maxLength
  override def validate(): ValidatedNel[String, CustomerFullName] =
    condNel(value.nonEmpty, this, "Il nome completo non può essere vuoto")
      .andThen(_ =>
        condNel(value.length <= maxLength, this, s"Il nome completo può essere lungo al massimo $maxLength caratteri")
      )

}

object CustomerFullName {

  private def capitalizeSingleWords(value: String): String = value.split("\\s+").map(_.capitalize).mkString(" ")

  def apply(value: String): CustomerFullName = new CustomerFullName(capitalizeSingleWords(value.trim.toLowerCase))

  implicit val CustomerFullNameEncoder: Encoder[CustomerFullName] = Encoder.encodeString.contramap(_.value)
  implicit val CustomerFullNameDecoder: Decoder[CustomerFullName] = Decoder.decodeString.map(CustomerFullName.apply)

}
