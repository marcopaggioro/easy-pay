package it.marcopaggioro.easypay.actor.projection

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.jdbc.scaladsl.JdbcProjection
import akka.projection.scaladsl.{GroupedProjection, Handler, SourceProvider}
import akka.projection.{ProjectionBehavior, ProjectionId}
import com.typesafe.scalalogging.LazyLogging
import it.marcopaggioro.easypay.AppConfig
import it.marcopaggioro.easypay.actor.TransactionsManagerActor
import it.marcopaggioro.easypay.database.PlainJdbcSession
import it.marcopaggioro.easypay.database.transactionshistory.{TransactionsHistoryRecord, TransactionsHistoryTable}
import slick.dbio.Effect
import slick.jdbc.JdbcBackend.Database
import it.marcopaggioro.easypay.database.PostgresProfile.api.*
import it.marcopaggioro.easypay.domain.TransactionsManager
import it.marcopaggioro.easypay.domain.TransactionsManager.{MoneyTransferred, TransactionsManagerEvent}
import slick.sql.FixedSqlAction

import java.time.{Duration, Instant}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

private class TransactionsProjectorActor(database: Database, system: ActorSystem[Nothing])
    extends Handler[Seq[EventEnvelope[TransactionsManagerEvent]]]
    with LazyLogging {

  private implicit lazy val executionContext: ExecutionContext = system.executionContext

  override def process(envelope: Seq[EventEnvelope[TransactionsManagerEvent]]): Future[Done] = {
    val startTime: Instant = Instant.now()
    logger.trace(s"Projecting ${envelope.size} events")

    val records: List[TransactionsHistoryRecord] = envelope.toList.flatMap { eventEnvelope =>
      eventEnvelope.event match {
        case TransactionsManager.WalletRecharged(transactionId, customerId, amount, instant) =>
          List(TransactionsHistoryRecord(transactionId, customerId, None, instant, amount))

        case MoneyTransferred(customerId, recipientCustomerId, transactionId, amount, instant) =>
          List(TransactionsHistoryRecord(transactionId, customerId, Some(recipientCustomerId), instant, amount))

        case _ => List.empty
      }
    }

    database
      .run(TransactionsHistoryTable.Table.insertOrUpdateAll(records))
      .transform {
        case Success(_) =>
          logger.debug(s"Projected ${envelope.size} events in ${Duration.between(startTime, Instant.now()).toString}")
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
      .withGroup(groupAfterEnvelopes = 500, groupAfterDuration = 1.second)

    system.systemActorOf(
      Behaviors.supervise(ProjectionBehavior(projection)).onFailure[Exception](SupervisorStrategy.restart),
      s"${TransactionsManagerActor.Name}-projector"
    )
  }

}
