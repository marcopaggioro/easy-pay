package it.marcopaggioro.easypay.domain

import akka.Done
import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import cats.data.Validated.{condNel, invalidNel, validNel}
import cats.data.ValidatedNel
import it.marcopaggioro.easypay.AppConfig
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerId, ScheduledOperationId, TransactionId}
import it.marcopaggioro.easypay.domain.classes.Domain.{DomainCommand, DomainEvent, DomainState}
import it.marcopaggioro.easypay.domain.classes.{Money, ScheduledOperation, Status}
import it.marcopaggioro.easypay.utilities.ValidationUtilities.{
  differentCustomerIdsValidation,
  validateAmount,
  validateDescription
}

import java.time.{Instant, Period}

object TransactionsManager {
  case class TransactionsManagerState(
      balances: Map[CustomerId, Money],
      scheduledOperations: Map[ScheduledOperationId, ScheduledOperation]
  ) extends DomainState

  sealed trait TransactionsManagerEvent extends DomainEvent[TransactionsManagerState]

  sealed trait TransactionsManagerCommand extends DomainCommand[TransactionsManagerState, TransactionsManagerEvent]

  // -----
  case class RechargeWallet(transactionId: TransactionId, customerId: CustomerId, amount: Money)(
      val replyTo: ActorRef[StatusReply[Done]]
  ) extends TransactionsManagerCommand {
    override def validate(state: TransactionsManagerState): ValidatedNel[String, Unit] = validateAmount(amount)

    override protected def generateEvents(state: TransactionsManagerState): List[TransactionsManagerEvent] = List(
      WalletRecharged(transactionId, customerId, amount)
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
      transactionId: TransactionId,
      description: String,
      amount: Money
  )(val replyTo: ActorRef[StatusReply[Done]])
      extends TransactionsManagerCommand {
    private def customerHasSufficientBalance(state: TransactionsManagerState): ValidatedNel[String, Unit] = condNel(
      state.balances.getOrElse(customerId, Money(0)).value >= amount.value,
      (),
      s"Non ci sono fondi sufficienti"
    )

    override def validate(state: TransactionsManagerState): ValidatedNel[String, Unit] =
      validateAmount(amount)
        .andThen(_ => customerHasSufficientBalance(state))
        .andThen(_ => differentCustomerIdsValidation(customerId, recipientCustomerId))
        .andThen(_ => validateDescription(description))

    override protected def generateEvents(state: TransactionsManagerState): List[TransactionsManagerEvent] = List(
      MoneyTransferred(customerId, recipientCustomerId, transactionId, description, amount)
    )
  }

  case class MoneyTransferred(
      customerId: CustomerId,
      recipientCustomerId: CustomerId,
      transactionId: TransactionId,
      description: String,
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

  // -----
  case class CreateScheduledOperation(scheduledOperationId: ScheduledOperationId, scheduledOperation: ScheduledOperation)(
      val replyTo: ActorRef[StatusReply[Done]]
  ) extends TransactionsManagerCommand {

    override def validate(state: TransactionsManagerState): ValidatedNel[String, Unit] =
      condNel(!state.scheduledOperations.contains(scheduledOperationId), (), "L'operazione pianificata esiste già")
        .andThen(_ => scheduledOperation.resetSeconds().validate().map(_ => ()))

    override protected def generateEvents(state: TransactionsManagerState): List[TransactionsManagerEvent] = List(
      ScheduledOperationCreated(scheduledOperationId, scheduledOperation.resetSeconds())
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
            "Non puoi eliminare questa operazione pianificata"
          )

        case None =>
          invalidNel("Operazione pianificata non trovata")
      }

    override protected def generateEvents(state: TransactionsManagerState): List[TransactionsManagerEvent] = List(
      ScheduledOperationDeleted(customerId, scheduledOperationId)
    )
  }

  case class ScheduledOperationDeleted(
      customerId: CustomerId,
      scheduledOperationId: ScheduledOperationId,
      override val instant: Instant = Instant.now()
  ) extends TransactionsManagerEvent {
    override def applyTo(state: TransactionsManagerState): TransactionsManagerState =
      state.copy(scheduledOperations = state.scheduledOperations.removed(scheduledOperationId))
  }

  // -----
  case class ExecuteScheduledOperations() extends TransactionsManagerCommand {
    override def validate(state: TransactionsManagerState): ValidatedNel[String, Unit] = validNel(())
  }

  // -----
  case class ScheduledOperationFeedback(scheduledOperationId: ScheduledOperationId, maybeError: Option[String])
      extends TransactionsManagerCommand {
    override def validate(state: TransactionsManagerState): ValidatedNel[String, Unit] = validNel(())

    override def generateEvents(state: TransactionsManagerState): List[TransactionsManagerEvent] = {
      val status: Status = maybeError match {
        case Some(error) => Status.Failed(error)
        case None        => Status.Done()
      }

      state.scheduledOperations.get(scheduledOperationId) match {
        case None =>
          // Scheduled operation does not exist: no generate events
          List.empty

        case Some(ScheduledOperation(_, _, _, _, _, _, _: Status.Failed)) =>
          // Scheduled operation exists and already has failed status: do not generate other events
          List.empty

        case Some(scheduledOperation) =>
          List(
            ScheduledOperationExecuted(scheduledOperationId, scheduledOperation, status)
          )
      }
    }
  }

  case class ScheduledOperationExecuted(
      scheduledOperationId: ScheduledOperationId,
      scheduledOperation: ScheduledOperation,
      nextStatus: Status,
      override val instant: Instant = Instant.now()
  ) extends TransactionsManagerEvent {

    override def applyTo(state: TransactionsManagerState): TransactionsManagerState =
      state.scheduledOperations.get(scheduledOperationId) match {
        case Some(scheduledOperation) =>
          nextStatus match {
            case _: Status.Pending =>
              state

            case _: Status.Done =>
              scheduledOperation.repeat match {
                case Some(repeatPeriod) =>
                  val updatedWhen: Instant = scheduledOperation.when.atZone(AppConfig.romeZoneId).plus(repeatPeriod).toInstant
                  state.copy(scheduledOperations =
                    state.scheduledOperations.updated(scheduledOperationId, scheduledOperation.copy(when = updatedWhen))
                  )

                case None =>
                  state.copy(scheduledOperations = state.scheduledOperations.removed(scheduledOperationId))
              }

            case failed: Status.Failed =>
              state.copy(scheduledOperations =
                state.scheduledOperations.updated(scheduledOperationId, scheduledOperation.copy(status = nextStatus))
              )
          }

        case None =>
          state
      }
  }
}
