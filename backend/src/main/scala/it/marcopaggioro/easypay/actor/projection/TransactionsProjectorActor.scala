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
import it.marcopaggioro.easypay.database.PostgresProfile.api._
import it.marcopaggioro.easypay.database.transactionshistory.{TransactionsHistoryRecord, TransactionsHistoryTable}
import it.marcopaggioro.easypay.domain.TransactionsManager
import it.marcopaggioro.easypay.domain.TransactionsManager.{MoneyTransferred, TransactionsManagerEvent, WalletRecharged}
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

  override def process(envelope: Seq[EventEnvelope[TransactionsManagerEvent]]): Future[Done] = {
    val startTime: Instant = Instant.now()
    logger.debug(s"Projecting ${envelope.size} events")

    val operations = envelope.toList.collect { eventEnvelope =>
      eventEnvelope.event match {
        case WalletRecharged(transactionId, customerId, amount, instant) =>
          TransactionsHistoryTable.Table += TransactionsHistoryRecord(
            transactionId,
            customerId,
            customerId,
            None,
            instant,
            amount
          )

        case MoneyTransferred(customerId, recipientCustomerId, transactionId, description, amount, instant) =>
          TransactionsHistoryTable.Table += TransactionsHistoryRecord(
            transactionId,
            customerId,
            recipientCustomerId,
            Some(description),
            instant,
            amount
          )
      }
    }

    database
      .run(DBIO.sequence(operations))
      .transform {
        case Success(operationsCount) =>
          logger.debug(s"Projected ${operationsCount.sum} events in ${Duration.between(startTime, Instant.now()).toString}")
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
