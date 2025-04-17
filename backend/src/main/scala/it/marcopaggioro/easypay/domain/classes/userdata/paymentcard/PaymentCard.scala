package it.marcopaggioro.easypay.domain.classes.userdata.paymentcard

import cats.data.ValidatedNel
import io.circe.Decoder
import it.marcopaggioro.easypay.domain.classes.Validable
import it.marcopaggioro.easypay.domain.classes.userdata._
import it.marcopaggioro.easypay.domain.classes.userdata.paymentcard.CardNumber.CardNumberDecoder
import it.marcopaggioro.easypay.utilities.ValidationUtilities.validateCardExpiration

import java.time.YearMonth

case class PaymentCard(
    fullName: CustomerFullName,
    cardNumber: CardNumber,
    expiration: YearMonth,
    securityCode: SecurityCode
) extends Validable[PaymentCard] {
  override def validate(): ValidatedNel[String, PaymentCard] = fullName
    .validate()
    .andThen(_ => cardNumber.validate())
    .andThen(_ => validateCardExpiration(expiration))
    .andThen(_ => securityCode.validate())
    .map(_ => this)
}

object PaymentCard {

  implicit val PaymentCardDecoder: Decoder[PaymentCard] = Decoder.instance { paymentCard =>
    for {
      fullName <- paymentCard.get[CustomerFullName]("fullName")
      cardNumber <- paymentCard.get[CardNumber]("cardNumber")
      expiration <- paymentCard.get[YearMonth]("expiration")
      securityCode <- paymentCard.get[SecurityCode]("securityCode")
    } yield PaymentCard(fullName, cardNumber, expiration, securityCode)
  }

}
