package it.marcopaggioro.easypay.domain.classes.userdata.paymentcard

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.Decoder
import it.marcopaggioro.easypay.domain.classes.Validable

import scala.util.matching.compat.Regex

case class CardNumber(value: String) extends Validable[CardNumber] {

  private lazy val cardNumberRegex: Regex = new Regex("^\\d{16}$")
  override def validate(): ValidatedNel[String, CardNumber] =
    condNel(cardNumberRegex.matches(value), this, "Numero della carta invalido")

  lazy val blurred: String = s"XXXX-XXXX-XXXX-${value.takeRight(4)}"

}

object CardNumber {

  implicit val CardNumberDecoder: Decoder[CardNumber] = Decoder.decodeString
    .map(CardNumber.apply)
    .or(Decoder.decodeLong.map(int => CardNumber(s"$int")))

}
