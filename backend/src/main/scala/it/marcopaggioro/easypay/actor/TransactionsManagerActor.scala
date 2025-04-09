package it.marcopaggioro.easypay.actor

import akka.Done
import akka.actor.typed.{Behavior, Scheduler}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.pattern.StatusReply
import akka.persistence.typed.scaladsl.Effect
import cats.data.Validated.{Invalid, Valid}
import it.marcopaggioro.easypay.actor.UsersManagerActor.standardCommandHandler
import it.marcopaggioro.easypay.domain.TransactionsManager
import it.marcopaggioro.easypay.domain.TransactionsManager.{
  CreateScheduledOperation,
  DeleteScheduledOperation,
  ExecuteScheduledOperations,
  GetBalance,
  GetScheduledOperations,
  RechargeWallet,
  ScheduledOperationExecuted,
  ScheduledOperationFeedback,
  TransactionsManagerCommand,
  TransactionsManagerEvent,
  TransactionsManagerState,
  TransferMoney
}
import it.marcopaggioro.easypay.AppConfig.{askTimeout, config}
import akka.actor.typed.scaladsl.AskPattern.*
import it.marcopaggioro.easypay.domain.classes.Aliases.ScheduledOperationId
import it.marcopaggioro.easypay.domain.classes.Money

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}
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

      case getBalance: GetBalance =>
        standardCommandHandler(
          context,
          state,
          command,
          _ => Effect.reply(getBalance.replyTo)(StatusReply.Success(state.balances.getOrElse(getBalance.customerId, Money(0)))),
          errors => defaultInvalidHandler(context, command, errors, getBalance.replyTo)
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

      case getScheduledOperations: GetScheduledOperations =>
        Effect.reply(getScheduledOperations.replyTo)(StatusReply.success(getScheduledOperations.getOperations(state)))

      case scheduledOperationFeedback: ScheduledOperationFeedback =>
        scheduledOperationFeedback.maybeError match {
          case Some(error) =>
            Effect.none.thenRun(_ => context.log.error(error))

          case None =>
            Effect.persist(scheduledOperationFeedback.generateEvents(state))
        }

      case executeOperations: ExecuteScheduledOperations =>
        Effect.none.thenRun { _ =>
          state.scheduledOperations.foreach {
            case (scheduledOperationId, scheduledOperation) if scheduledOperation.when.isBefore(LocalDateTime.now()) =>
              val transfer: Future[Done] = context.self.askWithStatus[Done](replyTo =>
                TransferMoney(
                  scheduledOperation.recipientCustomerId,
                  scheduledOperation.recipientCustomerId,
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
      FiniteDuration.apply(1, TimeUnit.HOURS)
    )

    super.apply(Name, TransactionsManagerState(Map.empty, Map.empty), commandHandler, eventHandler, _ => Set(EventTag))
  }

}
