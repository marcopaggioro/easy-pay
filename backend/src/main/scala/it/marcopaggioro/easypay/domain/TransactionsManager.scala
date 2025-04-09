package it.marcopaggioro.easypay.domain

import akka.Done
import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import cats.data.Validated.{condNel, validNel}
import cats.data.ValidatedNel
import it.marcopaggioro.easypay.domain.TransactionsManager.TransactionsManagerEvent
import it.marcopaggioro.easypay.domain.UsersManager.{UsersManagerEvent, UsersManagerState}
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerId, ScheduledOperationId, TransactionId}
import it.marcopaggioro.easypay.domain.classes.Domain.{DomainCommand, DomainEvent, DomainState}
import it.marcopaggioro.easypay.domain.classes.{Money, ScheduledOperation}
import it.marcopaggioro.easypay.utilities.ValidationUtilities.{differentCustomerIdsValidation, validatePositiveAmount}

import java.time.{Instant, LocalDateTime, Period}
import java.util.UUID

object TransactionsManager {
  case class TransactionsManagerState(
      balances: Map[CustomerId, Money],
      scheduledOperations: Map[ScheduledOperationId, ScheduledOperation]
  ) extends DomainState

  sealed trait TransactionsManagerEvent extends DomainEvent[TransactionsManagerState]

  sealed trait TransactionsManagerCommand extends DomainCommand[TransactionsManagerState, TransactionsManagerEvent]

  // -----
  case class RechargeWallet(customerId: CustomerId, amount: Money)(val replyTo: ActorRef[StatusReply[Done]])
      extends TransactionsManagerCommand {
    override def validate(state: TransactionsManagerState): ValidatedNel[String, Unit] = validatePositiveAmount(amount)

    override protected def generateEvents(state: TransactionsManagerState): List[TransactionsManagerEvent] = List(
      WalletRecharged(UUID.randomUUID(), customerId, amount)
    )
  }

  case class WalletRecharged(
      transactionId: TransactionId,
      customerId: CustomerId,
      amount: Money,
      override val instant: Instant = Instant.now()
  ) extends TransactionsManagerEvent {
    override def applyTo(state: TransactionsManagerState): TransactionsManagerState = {
      val currentBalance: Money = state.balances.getOrElse(customerId, Money(0))
      state.copy(balances =
        state.balances
          .updated(customerId, currentBalance.plus(amount))
      )
    }
  }

  // -----
  case class TransferMoney(
      customerId: CustomerId,
      recipientCustomerId: CustomerId,
      amount: Money
  )(val replyTo: ActorRef[StatusReply[Done]])
      extends TransactionsManagerCommand {
    private def customerHasSufficientBalance(state: TransactionsManagerState): ValidatedNel[String, Unit] = condNel(
      state.balances.getOrElse(customerId, Money(0)).value >= amount.value,
      (),
      s"Customer does not have sufficient balance"
    )

    override def validate(state: TransactionsManagerState): ValidatedNel[String, Unit] =
      validatePositiveAmount(amount)
        .andThen(_ => customerHasSufficientBalance(state))
        .andThen(_ => differentCustomerIdsValidation(customerId, recipientCustomerId))

    override protected def generateEvents(state: TransactionsManagerState): List[TransactionsManagerEvent] = List(
      MoneyTransferred(customerId, recipientCustomerId, UUID.randomUUID(), amount)
    )
  }

  case class MoneyTransferred(
      customerId: CustomerId,
      recipientCustomerId: CustomerId,
      transactionId: TransactionId,
      amount: Money,
      override val instant: Instant = Instant.now()
  ) extends TransactionsManagerEvent {
    override def applyTo(state: TransactionsManagerState): TransactionsManagerState = {
      val currentSenderBalance: Money = state.balances.getOrElse(customerId, Money(0))
      val currentRecipientBalance: Money = state.balances.getOrElse(recipientCustomerId, Money(0))
      state.copy(balances =
        state.balances
          .updated(customerId, currentSenderBalance.less(amount))
          .updated(recipientCustomerId, currentRecipientBalance.plus(amount))
      )
    }
  }

  case class GetBalance(customerId: CustomerId)(val replyTo: ActorRef[StatusReply[Money]]) extends TransactionsManagerCommand {
    override def validate(state: TransactionsManagerState): ValidatedNel[String, Unit] = validNel(())
  }

  // -----
  case class CreateScheduledOperation(scheduledOperation: ScheduledOperation)(val replyTo: ActorRef[StatusReply[Done]])
      extends TransactionsManagerCommand {
    override def validate(state: TransactionsManagerState): ValidatedNel[String, Unit] =
      scheduledOperation.validate().map(_ => ())

    override protected def generateEvents(state: TransactionsManagerState): List[TransactionsManagerEvent] = List(
      ScheduledOperationCreated(UUID.randomUUID(), scheduledOperation)
    )
  }

  case class ScheduledOperationCreated(
      scheduledOperationId: ScheduledOperationId,
      scheduledOperation: ScheduledOperation,
      override val instant: Instant = Instant.now()
  ) extends TransactionsManagerEvent {
    override def applyTo(state: TransactionsManagerState): TransactionsManagerState =
      state.copy(scheduledOperations = state.scheduledOperations.updated(scheduledOperationId, scheduledOperation))
  }

  // -----
  case class DeleteScheduledOperation(customerId: CustomerId, scheduledOperationId: ScheduledOperationId)(
      val replyTo: ActorRef[StatusReply[Done]]
  ) extends TransactionsManagerCommand {

    override def validate(state: TransactionsManagerState): ValidatedNel[String, Unit] =
      state.scheduledOperations.get(scheduledOperationId) match {
        case Some(scheduledOperation) =>
          condNel(
            scheduledOperation.senderCustomerId == customerId,
            (),
            "Can not delete this scheduled operation"
          )

        case None =>
          validNel(())
      }

    override protected def generateEvents(state: TransactionsManagerState): List[TransactionsManagerEvent] =
      state.scheduledOperations.get(scheduledOperationId) match {
        case Some(_) =>
          List(ScheduledOperationDeleted(scheduledOperationId))

        case None =>
          List.empty
      }
  }

  case class ScheduledOperationDeleted(
      scheduledOperationId: ScheduledOperationId,
      override val instant: Instant = Instant.now()
  ) extends TransactionsManagerEvent {
    override def applyTo(state: TransactionsManagerState): TransactionsManagerState =
      state.copy(scheduledOperations = state.scheduledOperations.removed(scheduledOperationId))
  }

  // -----
  case class GetScheduledOperations(customerId: CustomerId)(
      val replyTo: ActorRef[StatusReply[Map[ScheduledOperationId, ScheduledOperation]]]
  ) extends TransactionsManagerCommand {
    def getOperations(state: TransactionsManagerState): Map[ScheduledOperationId, ScheduledOperation] =
      state.scheduledOperations.filter { case (scheduledOperationId, scheduledOperation) =>
        scheduledOperation.senderCustomerId == customerId
      }

    override def validate(state: TransactionsManagerState): ValidatedNel[String, Unit] = validNel(())
  }

  // -----
  case class ExecuteScheduledOperations() extends TransactionsManagerCommand {
    override def validate(state: TransactionsManagerState): ValidatedNel[String, Unit] = validNel(())
  }

  // -----
  case class ScheduledOperationFeedback(scheduledOperationId: ScheduledOperationId, maybeError: Option[String])
      extends TransactionsManagerCommand {
    override def validate(state: TransactionsManagerState): ValidatedNel[String, Unit] = validNel(())

    override def generateEvents(state: TransactionsManagerState): List[TransactionsManagerEvent] = if (maybeError.isEmpty) {
      List(
        ScheduledOperationExecuted(scheduledOperationId)
      )
    } else {
      List.empty
    }
  }

  case class ScheduledOperationExecuted(
      scheduledOperationId: ScheduledOperationId,
      override val instant: Instant = Instant.now()
  ) extends TransactionsManagerEvent {

    override def applyTo(state: TransactionsManagerState): TransactionsManagerState =
      state.scheduledOperations.get(scheduledOperationId) match {
        case Some(scheduledOperation) =>
          scheduledOperation.repeat match {
            case Some(repeatPeriod) =>
              state.copy(scheduledOperations =
                state.scheduledOperations
                  .updated(scheduledOperationId, scheduledOperation.copy(when = scheduledOperation.when.plus(repeatPeriod)))
              )

            case None => state.copy(scheduledOperations = state.scheduledOperations.removed(scheduledOperationId))
          }

        case None =>
          state
      }
  }
}
