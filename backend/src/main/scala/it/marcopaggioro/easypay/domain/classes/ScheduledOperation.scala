package it.marcopaggioro.easypay.domain.classes

import cats.data.ValidatedNel
import cats.implicits.toTraverseOps
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.utilities.ValidationUtilities.{differentCustomerIdsValidation, validateDateTimeInFuture, validateDescription, validateMinimumPeriod, validatePositiveAmount}

import java.time.{LocalDateTime, Period}

case class ScheduledOperation(
    senderCustomerId: CustomerId,
    recipientCustomerId: CustomerId,
    amount: Money,
    when: LocalDateTime,
    description: String,
    repeat: Option[Period],
    status: Status = Status.Pending()
) extends Validable[ScheduledOperation] {
  override def validate(): ValidatedNel[String, ScheduledOperation] =
    differentCustomerIdsValidation(senderCustomerId, recipientCustomerId)
      .andThen(_ => validatePositiveAmount(amount))
      .andThen(_ => validateDateTimeInFuture(when))
      .andThen(_ => validateDescription(description))
      .andThen(_ => repeat.traverse(validateMinimumPeriod))
      .map(_ => this)

  def resetSeconds(): ScheduledOperation = copy(when = when.withSecond(0))
}

object ScheduledOperation {

  implicit val ScheduledOperationEncoder: Encoder[ScheduledOperation] = deriveEncoder[ScheduledOperation]

}
