package it.marcopaggioro.easypay.domain.classes

import cats.data.ValidatedNel
import cats.implicits.toTraverseOps
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.utilities.ValidationUtilities.{
  differentCustomerIdsValidation,
  validateDateTimeInFuture,
  validateMinimumPeriod,
  validatePositiveAmount
}

import java.time.{LocalDateTime, Period}

case class ScheduledOperation(
    senderCustomerId: CustomerId,
    recipientCustomerId: CustomerId,
    amount: Money,
    when: LocalDateTime,
    repeat: Option[Period]
) extends Validable[ScheduledOperation] {
  override def validate(): ValidatedNel[String, ScheduledOperation] =
    differentCustomerIdsValidation(senderCustomerId, recipientCustomerId)
      .andThen(_ => validatePositiveAmount(amount))
      .andThen(_ => validateDateTimeInFuture(when))
      .andThen(_ => repeat.traverse(validateMinimumPeriod))
      .map(_ => this)
}

object ScheduledOperation {

  implicit val ScheduledOperationEncoder: Encoder[ScheduledOperation] = deriveEncoder[ScheduledOperation]

}
