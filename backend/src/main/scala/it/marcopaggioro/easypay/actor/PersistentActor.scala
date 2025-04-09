package it.marcopaggioro.easypay.actor

import akka.Done
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, BackoffSupervisorStrategy, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria, SnapshotCountRetentionCriteria}
import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}
import it.marcopaggioro.easypay.domain.classes.Domain.{DomainCommand, DomainEvent, DomainState}

import scala.concurrent.duration.DurationInt

trait PersistentActor[Command <: DomainCommand[State, Event], Event <: DomainEvent[State], State <: DomainState] {

  protected lazy val retentionCriteria: SnapshotCountRetentionCriteria =
    RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2)

  protected lazy val persistFailureStrategy: BackoffSupervisorStrategy =
    SupervisorStrategy.restartWithBackoff(10.seconds, 60.seconds, 0.1)

  def defaultInvalidHandler[R](
      context: ActorContext[Command],
      command: Command,
      errors: NonEmptyList[String],
      replyTo: ActorRef[StatusReply[R]]
  ): ReplyEffect[Event, State] = {
    val errorsFlattened: String = errors.toList.mkString(", ")
    context.log.error(s"Failure during elaboration of command ${command.toString}: $errorsFlattened")
    Effect.reply(replyTo)(StatusReply.error(errorsFlattened))
  }

  def standardCommandHandler(
      context: ActorContext[Command],
      state: State,
      command: Command,
      onSuccess: List[Event] => Effect[Event, State],
      onFailure: NonEmptyList[String] => Effect[Event, State]
  ): Effect[Event, State] = command.validateAndGenerateEvents(state) match {
    case Valid(events) =>
      context.log.info(s"Generated ${events.size} events from command ${command.toString}")
      onSuccess(events)

    case Invalid(errors) =>
      onFailure(errors)
  }

  def handleWithPersistenceAndACK(
      context: ActorContext[Command],
      state: State,
      command: Command,
      replyTo: ActorRef[StatusReply[Done]]
  ): Effect[Event, State] =
    standardCommandHandler(
      context,
      state,
      command,
      events => Effect.persist(events).thenReply[StatusReply[Done]](replyTo)(_ => StatusReply.Ack),
      errors => defaultInvalidHandler[Done](context, command, errors, replyTo)
    )

  def apply(
      actorName: String,
      emptyState: State,
      commandHandler: (ActorContext[Command], State, Command) => Effect[Event, State],
      eventHandler: (State, Event) => State,
      tagger: Event => Set[String]
  ): Behavior[Command] = Behaviors.setup[Command] { context =>
    EventSourcedBehavior[Command, Event, State](
      PersistenceId.ofUniqueId(actorName),
      emptyState,
      commandHandler(context, _, _),
      eventHandler
    ).withTagger(tagger).withRetention(retentionCriteria).onPersistFailure(persistFailureStrategy).receiveSignal {
      case (_, signal) =>
        context.log.debug(s"[${context.self}] Received signal ${signal.getClass.getSimpleName}")
    }
  }

}
