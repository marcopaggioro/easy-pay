package it.marcopaggioro.easypay.domain.classes.userdata.paymentcard

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.Decoder
import it.marcopaggioro.easypay.domain.classes.Validable

case class SecurityCode(code: Int) extends Validable[SecurityCode] {

  override def validate(): ValidatedNel[String, SecurityCode] = condNel(code >= 100 || code <= 999, this, "CVV invalido")

}

object SecurityCode {

  implicit val SecurityCodeDecoder: Decoder[SecurityCode] = Decoder.decodeInt.map(SecurityCode.apply)

}
