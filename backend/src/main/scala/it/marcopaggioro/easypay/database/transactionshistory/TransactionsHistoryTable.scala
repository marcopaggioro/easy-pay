package it.marcopaggioro.easypay.database.transactionshistory

import slick.jdbc.H2Profile.Table
import slick.jdbc.JdbcType
import it.marcopaggioro.easypay.database.PostgresProfile.api.*
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerId, TransactionId}
import it.marcopaggioro.easypay.domain.classes.Money
import slick.ast.BaseTypedType
import slick.lifted.{PrimaryKey, ProvenShape, Rep, Tag}
import slick.lifted.{Rep, TableQuery, Tag, *}

import java.sql.Timestamp
import java.time.{Instant, OffsetDateTime, ZoneId, ZonedDateTime}
import java.util.UUID
import it.marcopaggioro.easypay.database.PostgresProfile.{EmailMapper, InstantMapper, MoneyMapper}
import it.marcopaggioro.easypay.domain.classes.userdata.Email

class TransactionsHistoryTable(tag: Tag) extends Table[TransactionsHistoryRecord](tag, "transactions_history") {

  def transactionId: Rep[TransactionId] = column[TransactionId]("transaction_id")
  def senderCustomerId: Rep[CustomerId] = column[CustomerId]("sender_customer_id")
  def recipientCustomerId: Rep[CustomerId] = column[CustomerId]("recipient_customer_id")
  def instant: Rep[Instant] = column[Instant]("instant")(InstantMapper)
  def amount: Rep[Money] = column[Money]("amount")

  override def * : ProvenShape[TransactionsHistoryRecord] =
    (transactionId, senderCustomerId, recipientCustomerId, instant, amount).mapTo[TransactionsHistoryRecord]

  def pk = primaryKey("pk_transactions_history", transactionId)

}

object TransactionsHistoryTable {

  lazy val Table: TableQuery[TransactionsHistoryTable] = TableQuery[TransactionsHistoryTable]

}
