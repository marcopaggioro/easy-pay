package it.marcopaggioro.easypay.routes.payloads.scheduledoperation

import cats.data.ValidatedNel
import cats.implicits.toTraverseOps
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.{Email, Money, Validable}
import it.marcopaggioro.easypay.routes.payloads.CreateUserPayload
import it.marcopaggioro.easypay.utilities.ValidationUtilities.{validatePositiveAmount, validateDateTimeInFuture, validateMinimumPeriod}

import java.time.{LocalDateTime, Period}

case class CreateScheduledOperationPayload(
    toCustomerEmail: Email,
    amount: Money,
    when: LocalDateTime,
    repeat: Option[Period]
) extends Validable[CreateScheduledOperationPayload] {
  override def validate(): ValidatedNel[String, CreateScheduledOperationPayload] =
    toCustomerEmail.validate()
      .andThen(_ => validatePositiveAmount(amount))
      .andThen(_ => validateDateTimeInFuture(when))
      .andThen(_ => repeat.traverse(validateMinimumPeriod))
      .map(_ => this)
}

object CreateScheduledOperationPayload {

  implicit val CreateScheduledOperationPayloadDecoder: Decoder[CreateScheduledOperationPayload] =
    deriveDecoder[CreateScheduledOperationPayload]

}
