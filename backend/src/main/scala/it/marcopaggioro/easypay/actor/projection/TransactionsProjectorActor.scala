package it.marcopaggioro.easypay.actor.projection

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler, SupervisorStrategy}
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.jdbc.scaladsl.JdbcProjection
import akka.projection.scaladsl.{GroupedProjection, Handler, SourceProvider}
import akka.projection.{ProjectionBehavior, ProjectionId}
import com.typesafe.scalalogging.LazyLogging
import it.marcopaggioro.easypay.actor.TransactionsManagerActor
import it.marcopaggioro.easypay.database.PlainJdbcSession
import it.marcopaggioro.easypay.database.PostgresProfile._
import it.marcopaggioro.easypay.database.PostgresProfile.api._
import it.marcopaggioro.easypay.database.scheduledoperations.{ScheduledOperationRecord, ScheduledOperationsTable}
import it.marcopaggioro.easypay.database.transactionshistory.{TransactionsHistoryRecord, TransactionsHistoryTable}
import it.marcopaggioro.easypay.database.usersbalance.{UserBalanceRecord, UsersBalanceTable}
import it.marcopaggioro.easypay.domain.TransactionsManager._
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerId, TransactionId}
import it.marcopaggioro.easypay.domain.classes.{Money, Status}
import slick.jdbc.JdbcBackend.Database

import java.time.{Duration, Instant}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

private class TransactionsProjectorActor(database: Database, system: ActorSystem[Nothing])
    extends Handler[Seq[EventEnvelope[TransactionsManagerEvent]]]
    with LazyLogging {

  private implicit lazy val executionContext: ExecutionContext = system.executionContext
  private implicit lazy val scheduler: Scheduler = system.scheduler

  private def getCurrentBalance(customerId: CustomerId) = UsersBalanceTable.Table
    .filter(_.customerId === customerId)
    .map(_.balance)
    .result
    .headOption
    .map(_.getOrElse(Money(0)))

  private def updateBalance(customerId: CustomerId, amount: Money) = for {
    currentBalance <- getCurrentBalance(customerId)
    updateBalance <- UsersBalanceTable.Table.insertOrUpdate(UserBalanceRecord(customerId, currentBalance.plus(amount)))
  } yield updateBalance

  private def moneyOperations(
      senderCustomerId: CustomerId,
      recipientCustomerId: CustomerId,
      transactionId: TransactionId,
      amount: Money,
      instant: Instant,
      description: Option[String]
  ) = {
    val updateBalances = if (senderCustomerId != recipientCustomerId) {
      val senderBalanceUpdate = updateBalance(senderCustomerId, -amount)
      val recipientBalanceUpdate = updateBalance(recipientCustomerId, amount)
      DBIO.sequence(List(senderBalanceUpdate, recipientBalanceUpdate)).map(_.sum)
    } else {
      updateBalance(recipientCustomerId, amount)
    }
    val updateHistory = TransactionsHistoryTable.Table.insertOrUpdate(
      TransactionsHistoryRecord(
        transactionId,
        senderCustomerId,
        recipientCustomerId,
        description,
        instant,
        amount
      )
    )
    DBIO.sequence(List(updateBalances, updateHistory)).map(_.sum)
  }

  override def process(envelope: Seq[EventEnvelope[TransactionsManagerEvent]]): Future[Done] = {
    val startTime: Instant = Instant.now()
    logger.debug(s"Projecting ${envelope.size} events")

    val operations = envelope.toList.map { eventEnvelope =>
      eventEnvelope.event match {
        case WalletRecharged(transactionId, customerId, amount, instant) =>
          moneyOperations(customerId, customerId, transactionId, amount, instant, None)

        case MoneyTransferred(customerId, recipientCustomerId, transactionId, description, amount, instant) =>
          moneyOperations(customerId, recipientCustomerId, transactionId, amount, instant, Some(description))

        case ScheduledOperationCreated(scheduledOperationId, scheduledOperation, _) =>
          ScheduledOperationsTable.Table.insertOrUpdate(
            ScheduledOperationRecord(
              scheduledOperationId,
              scheduledOperation.senderCustomerId,
              scheduledOperation.recipientCustomerId,
              scheduledOperation.description,
              scheduledOperation.when,
              scheduledOperation.amount,
              scheduledOperation.repeat,
              scheduledOperation.status.code
            )
          )

        case ScheduledOperationDeleted(scheduledOperationId, instant) =>
          ScheduledOperationsTable.Table.filter(_.scheduledOperationId === scheduledOperationId).delete

        case ScheduledOperationExecuted(scheduledOperationId, scheduledOperation, nextStatus, instant) =>
          nextStatus match {
            case failed: Status.Failed =>
              ScheduledOperationsTable.Table
                .filter(_.scheduledOperationId === scheduledOperationId)
                .map(_.status)
                .update(failed.code)

            case _ =>
              scheduledOperation.repeat match {
                case Some(repeat) =>
                  ScheduledOperationsTable.Table
                    .filter(_.scheduledOperationId === scheduledOperationId)
                    .map(_.when)
                    .update(scheduledOperation.when.plus(repeat))

                case None =>
                  ScheduledOperationsTable.Table.filter(_.scheduledOperationId === scheduledOperationId).delete
              }
          }
      }
    }

    database
      .run(DBIO.sequence(operations))
      .transform {
        case Success(operationsCount) =>
          logger.debug(s"Executed ${operationsCount.sum} operations in ${Duration.between(startTime, Instant.now()).toString}")
          Success(Done)

        case Failure(throwable) =>
          logger.error("Projection error", throwable)
          Failure(throwable)
      }
  }

}

object TransactionsProjectorActor {

  def startProjectorActor(database: Database, system: ActorSystem[Nothing]): ActorRef[ProjectionBehavior.Command] = {
    val sourceProvider: SourceProvider[Offset, EventEnvelope[TransactionsManagerEvent]] = EventSourcedProvider
      .eventsByTag[TransactionsManagerEvent](system, JdbcReadJournal.Identifier, TransactionsManagerActor.EventTag)

    val projection: GroupedProjection[Offset, EventEnvelope[TransactionsManagerEvent]] = JdbcProjection
      .groupedWithinAsync(
        ProjectionId(s"${TransactionsManagerActor.Name}-jdbc-projection", TransactionsManagerActor.EventTag),
        sourceProvider,
        () => new PlainJdbcSession,
        () => new TransactionsProjectorActor(database, system)
      )(system)
      .withGroup(groupAfterEnvelopes = 100, groupAfterDuration = 100.milliseconds)

    system.systemActorOf(
      Behaviors.supervise(ProjectionBehavior(projection)).onFailure[Exception](SupervisorStrategy.restart),
      s"${TransactionsManagerActor.Name}-projector"
    )
  }

}
