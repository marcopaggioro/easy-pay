package it.marcopaggioro.easypay.database.transactionshistory

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerId, TransactionId}
import it.marcopaggioro.easypay.domain.classes.Money

import java.time.Instant

final case class TransactionsHistoryRecord(
    transactionId: TransactionId,
    customerId: CustomerId,
    recipientCustomerId: Option[CustomerId],
    instant: Instant,
    amount: Money
)

object TransactionsHistoryRecord {

  implicit val TransactionsHistoryRecordEncoder: Encoder[TransactionsHistoryRecord] = deriveEncoder[TransactionsHistoryRecord]

}
