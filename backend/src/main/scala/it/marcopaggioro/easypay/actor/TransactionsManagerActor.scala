package it.marcopaggioro.easypay.actor

import akka.Done
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{Behavior, Scheduler}
import akka.persistence.typed.scaladsl.Effect
import cats.data.Validated.{Invalid, Valid}
import it.marcopaggioro.easypay.AppConfig.askTimeout
import it.marcopaggioro.easypay.domain.TransactionsManager
import it.marcopaggioro.easypay.domain.TransactionsManager.{
  CreateScheduledOperation,
  DeleteScheduledOperation,
  ExecuteScheduledOperations,
  RechargeWallet,
  ScheduledOperationFeedback,
  TransactionsManagerCommand,
  TransactionsManagerEvent,
  TransactionsManagerState,
  TransferMoney
}

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object TransactionsManagerActor
    extends PersistentActor[TransactionsManagerCommand, TransactionsManagerEvent, TransactionsManagerState] {

  val Name: String = "transactions-manager-actor"
  val EventTag: String = "transaction"

  private def eventHandler(state: TransactionsManagerState, event: TransactionsManagerEvent): TransactionsManagerState =
    event.applyTo(state)

  private def commandHandler(
      context: ActorContext[TransactionsManagerCommand],
      state: TransactionsManagerState,
      command: TransactionsManagerCommand
  ): Effect[TransactionsManagerEvent, TransactionsManagerState] = {
    implicit val scheduler: Scheduler = context.system.scheduler
    implicit val executionContext: ExecutionContext = context.executionContext
    context.log.debug(s"Received command $command")

    command match {
      case rechargeWallet: RechargeWallet =>
        handleWithPersistenceAndACK(context, state, command, rechargeWallet.replyTo)

      case transferMoney: TransferMoney =>
        handleWithPersistenceAndACK(
          context,
          state,
          command,
          transferMoney.replyTo
        )

      case create: CreateScheduledOperation =>
        handleWithPersistenceAndACK(
          context,
          state,
          command,
          create.replyTo
        )

      case delete: DeleteScheduledOperation =>
        handleWithPersistenceAndACK(
          context,
          state,
          command,
          delete.replyTo
        )

      case scheduledOperationFeedback: ScheduledOperationFeedback =>
        scheduledOperationFeedback.validateAndGenerateEvents(state) match {
          case Valid(events) => Effect.persist(events)

          case Invalid(errors) => Effect.none.thenRun(_ => context.log.error(errors.toList.mkString(", ")))
        }

      case executeOperations: ExecuteScheduledOperations =>
        Effect.none.thenRun { _ =>
          state.scheduledOperations.foreach {
            case (scheduledOperationId, scheduledOperation) if scheduledOperation.when.isBefore(Instant.now()) =>
              val transfer: Future[Done] = context.self.askWithStatus[Done](replyTo =>
                TransferMoney(
                  scheduledOperation.senderCustomerId,
                  scheduledOperation.recipientCustomerId,
                  UUID.randomUUID(),
                  scheduledOperation.description,
                  scheduledOperation.amount
                )(replyTo)
              )
              context.pipeToSelf(transfer) {
                case Failure(exception) =>
                  ScheduledOperationFeedback(scheduledOperationId, Some(exception.getMessage))

                case Success(value) =>
                  ScheduledOperationFeedback(scheduledOperationId, None)
              }

            case _ => ()
          }
        }

    }
  }

  def apply(): Behavior[TransactionsManagerCommand] = Behaviors.withTimers[TransactionsManagerCommand] { timerScheduler =>
    timerScheduler.startTimerAtFixedRate(
      ExecuteScheduledOperations(),
      FiniteDuration.apply(10, TimeUnit.SECONDS),
      FiniteDuration.apply(1, TimeUnit.MINUTES)
    )

    super.apply(Name, TransactionsManagerState(Map.empty, Map.empty), commandHandler, eventHandler, _ => Set(EventTag))
  }

}
