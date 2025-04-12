package it.marcopaggioro.easypay.database.scheduledoperations

import it.marcopaggioro.easypay.database.PostgresProfile.api.*
import it.marcopaggioro.easypay.database.PostgresProfile.MyAPI.date2PeriodTypeMapper
import it.marcopaggioro.easypay.database.PostgresProfile.{InstantMapper, MoneyMapper}
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerId, ScheduledOperationId, TransactionId}
import it.marcopaggioro.easypay.domain.classes.{Money, Status}
import slick.jdbc.H2Profile.Table
import slick.lifted.{Rep, TableQuery, Tag, *}

import java.time.{Instant, Period}

class ScheduledOperationsTable(tag: Tag) extends Table[ScheduledOperationRecord](tag, "scheduled_operations") {

  def scheduledOperationId: Rep[ScheduledOperationId] = column[ScheduledOperationId]("scheduled_operation_id")
  def senderCustomerId: Rep[CustomerId] = column[CustomerId]("sender_customer_id")
  def recipientCustomerId: Rep[CustomerId] = column[CustomerId]("recipient_customer_id")
  def description: Rep[String] = column[String]("description")
  def when: Rep[Instant] = column[Instant]("when")(InstantMapper)
  def amount: Rep[Money] = column[Money]("amount")
  def repeat: Rep[Option[Period]] = column[Option[Period]]("repeat")
  def status: Rep[String] = column[String]("status")

  override def * : ProvenShape[ScheduledOperationRecord] =
    (scheduledOperationId, senderCustomerId, recipientCustomerId, description, when, amount, repeat, status)
      .mapTo[ScheduledOperationRecord]

  def pk = primaryKey("pk_scheduled_operations", scheduledOperationId)

}

object ScheduledOperationsTable {

  lazy val Table: TableQuery[ScheduledOperationsTable] = TableQuery[ScheduledOperationsTable]

}
