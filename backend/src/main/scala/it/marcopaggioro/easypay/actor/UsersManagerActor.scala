package it.marcopaggioro.easypay.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.pattern.StatusReply
import akka.persistence.typed.scaladsl.Effect
import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated}
import it.marcopaggioro.easypay.actor.TransactionsManagerActor.handleWithPersistenceAndACK
import it.marcopaggioro.easypay.domain.UsersManager
import it.marcopaggioro.easypay.domain.UsersManager.{
  CreateUser,
  LoginUserWithEmail,
  UpdateUserData,
  UsersManagerCommand,
  UsersManagerEvent,
  UsersManagerState
}

object UsersManagerActor extends PersistentActor[UsersManagerCommand, UsersManagerEvent, UsersManagerState] {

  val Name: String = "users-manager-actor"
  val EventTag: String = "users-manager"

  private def eventHandler(state: UsersManagerState, event: UsersManagerEvent): UsersManagerState = event.applyTo(state)

  private def commandHandler(
      context: ActorContext[UsersManagerCommand],
      state: UsersManagerState,
      command: UsersManagerCommand
  ): Effect[UsersManagerEvent, UsersManagerState] = {
    context.log.debug(s"Received command $command")

    command match {
      case createUser: CreateUser =>
        standardCommandHandler(
          context,
          state,
          command,
          events => Effect.persist(events).thenReply(createUser.replyTo)(_ => StatusReply.Success(createUser.customerId)),
          errors => defaultInvalidHandler(context, command, errors, createUser.replyTo)
        )

      case loginUserWithEmail: LoginUserWithEmail =>
        loginUserWithEmail.validateAndGetCustomerId(state) match {
          case Valid(customerId) =>
            Effect.reply(loginUserWithEmail.replyTo)(StatusReply.Success(customerId))

          case Invalid(errors) =>
            defaultInvalidHandler(context, command, errors, loginUserWithEmail.replyTo)
        }

      case updateUserData: UpdateUserData =>
        handleWithPersistenceAndACK(context, state, command, updateUserData.replyTo)

    }

  }

  def apply(): Behavior[UsersManagerCommand] = super
    .apply(Name, UsersManagerState(Map.empty), commandHandler, eventHandler, _ => Set(EventTag))

}
