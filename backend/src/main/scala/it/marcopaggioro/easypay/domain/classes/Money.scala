package it.marcopaggioro.easypay.domain.classes

import io.circe.{Decoder, Encoder}

case class Money private (value: BigDecimal) {

  def plus(money: Money): Money = copy(value + money.value)
  def less(money: Money): Money = copy(value - money.value)

  def unary_- : Money = Money(-value)

}

object Money {
  private val scale: Int = 2

  def apply(amount: BigDecimal): Money = new Money(amount.setScale(scale, BigDecimal.RoundingMode.HALF_UP))
  def apply(amount: Int): Money = apply(BigDecimal(amount))

  implicit val MoneyEncoder: Encoder[Money] = Encoder.encodeBigDecimal.contramap(_.value)
  implicit val MoneyDecoder: Decoder[Money] = Decoder.decodeBigDecimal.map(Money.apply)

}
