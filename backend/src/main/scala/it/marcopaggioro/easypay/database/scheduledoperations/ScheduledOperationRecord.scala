package it.marcopaggioro.easypay.database.scheduledoperations

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import it.marcopaggioro.easypay.database.users.UserRecord
import it.marcopaggioro.easypay.database.users.UserRecord.UserRecordInteractedEncoder
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerId, ScheduledOperationId}
import it.marcopaggioro.easypay.domain.classes.Money

import java.time.{Instant, Period}

final case class ScheduledOperationRecord(
    scheduledOperationId: ScheduledOperationId,
    senderCustomerId: CustomerId,
    recipientCustomerId: CustomerId,
    description: String,
    when: Instant,
    amount: Money,
    repeat: Option[Period],
    status: String
)

object ScheduledOperationRecord {

  implicit val PeriodEncoder: Encoder[Period] = Encoder.instance { period =>
    val monthsString: String = period.getMonths match {
      case months if months > 1 => s"$months mesi"
      case 1                    => "1 mese"
      case 0                    => ""
    }
    val daysString: String = period.getDays match {
      case months if months > 1 => s"$months giorni"
      case 1                    => "1 giorno"
      case 0                    => ""
    }

    Seq(monthsString, daysString).filter(_.nonEmpty).mkString(" e ").asJson
  }

  implicit val ScheduledOperationRecordEncoder: Encoder[ScheduledOperationRecord] = Encoder.instance { record =>
    Json.obj(
      "id" -> record.scheduledOperationId.asJson,
      "senderCustomerId" -> record.senderCustomerId.asJson,
      "recipientCustomerId" -> record.recipientCustomerId.asJson,
      "description" -> record.description.asJson,
      "when" -> record.when.toEpochMilli.asJson,
      "amount" -> record.amount.asJson,
      "repeat" -> record.repeat.asJson,
      "status" -> record.status.asJson
    )
  }

  implicit val ScheduledOperationUserJoinEncoder: Encoder[(ScheduledOperationRecord, UserRecord)] = Encoder.instance {
    case (scheduledRecord, recipientRecord) =>
      scheduledRecord.asJson.deepMerge(recipientRecord.asJson(UserRecordInteractedEncoder))
  }

}
