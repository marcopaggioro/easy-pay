package it.marcopaggioro.easypay.database.scheduledoperations

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import it.marcopaggioro.easypay.database.users.UserRecord
import it.marcopaggioro.easypay.database.users.UserRecord.UserRecordInteractedEncoder
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerId, ScheduledOperationId, TransactionId}
import it.marcopaggioro.easypay.domain.classes.{Money, Status}

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

  implicit val ScheduledOperationRecordEncoder: Encoder[ScheduledOperationRecord] = Encoder.instance { record =>
    Json.obj(
      "id" -> record.scheduledOperationId.asJson,
      "senderCustomerId" -> record.senderCustomerId.asJson,
      "recipientCustomerId" -> record.recipientCustomerId.asJson,
      "description" -> record.description.asJson,
      "when" -> record.when.asJson,
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
