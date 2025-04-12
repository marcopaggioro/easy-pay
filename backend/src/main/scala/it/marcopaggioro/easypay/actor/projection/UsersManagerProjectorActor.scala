package it.marcopaggioro.easypay.actor.projection

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, SupervisorStrategy}
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.jdbc.scaladsl.JdbcProjection
import akka.projection.scaladsl.{GroupedProjection, Handler}
import akka.projection.{ProjectionBehavior, ProjectionId}
import com.typesafe.scalalogging.LazyLogging
import it.marcopaggioro.easypay.actor.UsersManagerActor
import it.marcopaggioro.easypay.database.PlainJdbcSession
import it.marcopaggioro.easypay.database.PostgresProfile.*
import it.marcopaggioro.easypay.database.PostgresProfile.api.*
import it.marcopaggioro.easypay.database.users.{UserRecord, UsersTable}
import it.marcopaggioro.easypay.domain.UsersManager.*
import it.marcopaggioro.easypay.domain.classes.Money
import slick.jdbc.JdbcBackend.Database

import java.time.{Duration, Instant}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

private class UsersManagerProjectorActor(
    database: Database,
    system: ActorSystem[Nothing]
) extends Handler[Seq[EventEnvelope[UsersManagerEvent]]]
    with LazyLogging {

  private implicit lazy val executionContext: ExecutionContext = system.executionContext

  override def process(envelope: Seq[EventEnvelope[UsersManagerEvent]]): Future[Done] = {
    val startTime: Instant = Instant.now()
    logger.debug(s"Projecting ${envelope.size} events")

    val operations = envelope.toList.collect { eventEnvelope =>
      eventEnvelope.event match
        case UserCreated(customerId, userData, _) =>
          UsersTable.Table.insertOrUpdate(
            UserRecord(customerId, userData.firstName, userData.lastName, userData.birthDate, userData.email)
          )

        case FirstChanged(customerId, firstName, _) =>
          UsersTable.Table.filter(_.customerId == customerId).map(_.firstName).update(firstName)

        case LastNameChanged(customerId, lastName, _) =>
          UsersTable.Table.filter(_.customerId == customerId).map(_.lastName).update(lastName)

        case EmailChanged(customerId, email, _) =>
          UsersTable.Table.filter(_.customerId == customerId).map(_.email).update(email)
    }

    database.run(DBIO.sequence(operations)).transform {
      case Success(operationsCount) =>
        logger.debug(s"Projected ${operationsCount.sum} events in ${Duration.between(startTime, Instant.now()).toString}")
        Success(Done)

      case Failure(throwable) =>
        logger.error("Projection error", throwable)
        Failure(throwable)
    }
  }

}

object UsersManagerProjectorActor {

  def startProjectorActor(
      database: Database,
      system: ActorSystem[Nothing]
  ): ActorRef[ProjectionBehavior.Command] = {
    val sourceProvider = EventSourcedProvider
      .eventsByTag[UsersManagerEvent](system, JdbcReadJournal.Identifier, UsersManagerActor.EventTag)

    val projection: GroupedProjection[Offset, EventEnvelope[UsersManagerEvent]] = JdbcProjection
      .groupedWithinAsync(
        ProjectionId(s"${UsersManagerActor.Name}-jdbc-projection", UsersManagerActor.EventTag),
        sourceProvider,
        () => new PlainJdbcSession,
        () => new UsersManagerProjectorActor(database, system)
      )(system)
      .withGroup(groupAfterEnvelopes = 100, groupAfterDuration = 100.milliseconds)

    system.systemActorOf(
      Behaviors.supervise(ProjectionBehavior(projection)).onFailure[Exception](SupervisorStrategy.restart),
      s"${UsersManagerActor.Name}-projector"
    )
  }

}
