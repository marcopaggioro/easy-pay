package it.marcopaggioro.easypay.database.transactionshistory

import it.marcopaggioro.easypay.database.PostgresProfile.api._
import it.marcopaggioro.easypay.database.PostgresProfile.{InstantMapper, MoneyMapper}
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerId, TransactionId}
import it.marcopaggioro.easypay.domain.classes.Money
import slick.jdbc.H2Profile.Table
import slick.lifted.{Rep, TableQuery, Tag, _}

import java.time.Instant
import java.util.UUID

class TransactionsHistoryTable(tag: Tag) extends Table[TransactionsHistoryRecord](tag, "transactions_history") {

  def transactionId: Rep[TransactionId] = column[TransactionId]("transaction_id")
  def senderCustomerId: Rep[CustomerId] = column[CustomerId]("sender_customer_id")
  def recipientCustomerId: Rep[CustomerId] = column[CustomerId]("recipient_customer_id")
  def description: Rep[Option[String]] = column[Option[String]]("description")
  def instant: Rep[Instant] = column[Instant]("instant")(InstantMapper)
  def amount: Rep[Money] = column[Money]("amount")

  override def * : ProvenShape[TransactionsHistoryRecord] =
    (transactionId, senderCustomerId, recipientCustomerId, description, instant, amount).mapTo[TransactionsHistoryRecord]

  def pk = primaryKey("pk_transactions_history", transactionId)

}

object TransactionsHistoryTable {

  lazy val Table: TableQuery[TransactionsHistoryTable] = TableQuery[TransactionsHistoryTable]

}
