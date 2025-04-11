package it.marcopaggioro.easypay.routes.payloads.scheduledoperation

import cats.data.ValidatedNel
import cats.implicits.toTraverseOps
import io.circe.Decoder
import it.marcopaggioro.easypay.domain.classes.userdata.Email
import it.marcopaggioro.easypay.domain.classes.{Money, Validable}
import it.marcopaggioro.easypay.utilities.ValidationUtilities.{
  validateDateTimeInFuture,
  validateDescription,
  validateMinimumPeriod,
  validatePositiveAmount
}

import java.time.{LocalDateTime, Period}

case class CreateScheduledOperationPayload(
    recipientEmail: Email,
    amount: Money,
    when: LocalDateTime,
    description: String,
    repeat: Option[Period]
) extends Validable[CreateScheduledOperationPayload] {
  override def validate(): ValidatedNel[String, CreateScheduledOperationPayload] =
    recipientEmail
      .validate()
      .andThen(_ => validatePositiveAmount(amount))
      .andThen(_ => validateDateTimeInFuture(when))
      .andThen(_ => validateDescription(description))
      .andThen(_ => repeat.traverse(validateMinimumPeriod))
      .map(_ => this)
}

object CreateScheduledOperationPayload {

  implicit val CreateScheduledOperationPayloadDecoder: Decoder[CreateScheduledOperationPayload] = Decoder.instance { cursor =>
    for {
      recipientEmail <- cursor.get[Email]("recipientEmail")
      amount <- cursor.get[Money]("amount")
      when <- cursor.get[LocalDateTime]("when")
      description <- cursor.get[String]("description")
      repeat <- cursor.get[Option[Period]]("repeat")
    } yield CreateScheduledOperationPayload(recipientEmail, amount, when, description, repeat)
  }

}
