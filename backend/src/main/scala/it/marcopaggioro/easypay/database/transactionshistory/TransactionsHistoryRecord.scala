package it.marcopaggioro.easypay.database.transactionshistory

import io.circe.{Encoder, Json}
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax.EncoderOps
import it.marcopaggioro.easypay.database.users.UserRecord
import it.marcopaggioro.easypay.database.users.UserRecord.UserRecordInteractedEncoder
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerId, TransactionId}
import it.marcopaggioro.easypay.domain.classes.userdata.{Email, UserData}
import it.marcopaggioro.easypay.domain.classes.Money

import java.time.Instant

final case class TransactionsHistoryRecord(
                                            transactionId: TransactionId,
                                            senderCustomerId: CustomerId,
                                            recipientCustomerId: CustomerId,
                                            instant: Instant,
                                            amount: Money
)

object TransactionsHistoryRecord {

  implicit val TransactionsHistoryRecordEncoder: Encoder[TransactionsHistoryRecord] = Encoder.instance { record =>
    Json.obj(
      "transactionId" -> record.transactionId.asJson,
      "senderCustomerId" -> record.senderCustomerId.asJson,
      "recipientCustomerId" -> record.recipientCustomerId.asJson,
      "instant" -> record.instant.asJson,
      "amount" -> record.amount.asJson
    )
  }

  implicit val TransactionUserJoinEncoder: Encoder[(TransactionsHistoryRecord, Option[UserRecord])] = Encoder.instance {
    case (transactionsHistoryRecord, maybeRecipientRecord) =>
      maybeRecipientRecord match {
        case Some(recipientRecord) =>
          transactionsHistoryRecord.asJson.deepMerge(recipientRecord.asJson(UserRecordInteractedEncoder))

        case None =>
          transactionsHistoryRecord.asJson
      }
  }

}
