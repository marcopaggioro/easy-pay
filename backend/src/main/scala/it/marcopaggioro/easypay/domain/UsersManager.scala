package it.marcopaggioro.easypay.domain

import akka.Done
import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import cats.data.Validated.{Invalid, Valid, condNel, invalidNel, validNel}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.implicits.toTraverseOps
import it.marcopaggioro.easypay.AppConfig
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.Domain.{DomainCommand, DomainEvent, DomainState}
import it.marcopaggioro.easypay.domain.classes.{Email, UserData}
import it.marcopaggioro.easypay.utilities.ValidationUtilities.{
  validateCustomerName,
  validateCustomerSurname,
  validateEncryptedPassword
}

import java.time.{Instant, ZonedDateTime}
import java.util.UUID

object UsersManager {

  case class UsersManagerState(users: Map[CustomerId, UserData]) extends DomainState {
    lazy val usersEmails: Map[Email, CustomerId] = users.map { case (customerId, userData) =>
      userData.email -> customerId
    }
  }

  sealed trait UsersManagerEvent extends DomainEvent[UsersManagerState] {
    val customerId: CustomerId
  }

  sealed trait UsersManagerCommand extends DomainCommand[UsersManagerState, UsersManagerEvent] {
    def customerIdExistsValidation(state: UsersManagerState, customerId: CustomerId): ValidatedNel[String, UserData] =
      state.users.get(customerId) match {
        case Some(userData) => validNel(userData)
        case None           => invalidNel(s"Customer id $customerId not exists")
      }

    def customerIdNotExistsValidation(state: UsersManagerState, customerId: CustomerId): ValidatedNel[String, Unit] =
      customerIdExistsValidation(state, customerId) match {
        case Valid(_)        => invalidNel(s"Customer id $customerId already exists")
        case Invalid(errors) => validNel(())
      }

    def customerEmailExistsValidation(state: UsersManagerState, email: Email): ValidatedNel[String, (CustomerId, UserData)] =
      state.users
        .collectFirst {
          case (customerId, userData) if userData.email == email => validNel(customerId -> userData)
        }
        .getOrElse(invalidNel(s"Email ${email.value} not exists"))

    def emailNotAlreadyExistsValidation(state: UsersManagerState, email: Email): ValidatedNel[String, Unit] =
      condNel(!state.usersEmails.contains(email), (), s"Email ${email.value} already registered")
  }

  // -----
  case class CreateUser(customerId: UUID, userData: UserData)(val replyTo: ActorRef[StatusReply[CustomerId]])
      extends UsersManagerCommand {
    override def validate(state: UsersManagerState): ValidatedNel[String, Unit] =
      userData
        .validate()
        .andThen(_ => customerIdNotExistsValidation(state, customerId))
        .andThen(_ => emailNotAlreadyExistsValidation(state, userData.email))

    override protected def generateEvents(state: UsersManagerState): List[UsersManagerEvent] = List(
      UserCreated(customerId, userData)
    )
  }

  case class UserCreated(
      override val customerId: CustomerId,
      userData: UserData,
      override val instant: Instant = Instant.now()
  ) extends UsersManagerEvent {
    override def applyTo(state: UsersManagerState): UsersManagerState =
      state.copy(users = state.users.updated(customerId, userData))
  }

  // -----
  case class UpdateUserData(
      customerId: CustomerId,
      maybeEmail: Option[Email],
      maybeName: Option[String],
      maybeSurname: Option[String],
      maybeEncryptedPassword: Option[String]
  )(val replyTo: ActorRef[StatusReply[Done]])
      extends UsersManagerCommand {
    override def validate(state: UsersManagerState): ValidatedNel[String, Unit] =
      customerIdExistsValidation(state, customerId)
        .andThen(_ => maybeEmail.traverse(email => email.validate().andThen(_ => emailNotAlreadyExistsValidation(state, email))))
        .andThen(_ => maybeName.traverse(validateCustomerName))
        .andThen(_ => maybeSurname.traverse(validateCustomerSurname))
        .andThen(_ => maybeEncryptedPassword.traverse(validateEncryptedPassword))
        .map(_ => ())

    override protected def generateEvents(state: UsersManagerState): List[UsersManagerEvent] =
      state.users.get(customerId) match {
        case None =>
          List.empty

        case Some(userData) =>
          val emailChanged: Option[EmailChanged] =
            maybeEmail.flatMap(email => Option.when(userData.email != email)(EmailChanged(customerId, email)))
          val nameChanged: Option[NameChanged] =
            maybeName.flatMap(name => Option.when(userData.name != name)(NameChanged(customerId, name)))
          val surnameChanged: Option[SurnameChanged] =
            maybeSurname.flatMap(surname => Option.when(userData.surname != surname)(SurnameChanged(customerId, surname)))
          val passwordChanged: Option[PasswordChanged] =
            maybeEncryptedPassword.flatMap(encryptedPassword =>
              Option.when(userData.encryptedPassword != encryptedPassword)(
                PasswordChanged(customerId, encryptedPassword)
              )
            )

          List(emailChanged, nameChanged, surnameChanged, passwordChanged).flatten
      }
  }

  case class NameChanged(override val customerId: CustomerId, name: String, override val instant: Instant = Instant.now())
      extends UsersManagerEvent {
    override def applyTo(state: UsersManagerState): UsersManagerState = state.users.get(customerId) match {
      case Some(currentUserData) => state.copy(users = state.users.updated(customerId, currentUserData.withName(name)))
      case None                  => state
    }

  }

  case class SurnameChanged(override val customerId: CustomerId, surname: String, override val instant: Instant = Instant.now())
      extends UsersManagerEvent {
    override def applyTo(state: UsersManagerState): UsersManagerState = state.users.get(customerId) match {
      case Some(currentUserData) => state.copy(users = state.users.updated(customerId, currentUserData.withSurname(surname)))
      case None                  => state
    }

  }

  case class EmailChanged(override val customerId: CustomerId, email: Email, override val instant: Instant = Instant.now())
      extends UsersManagerEvent {
    override def applyTo(state: UsersManagerState): UsersManagerState = state.users.get(customerId) match {
      case Some(currentUserData) => state.copy(users = state.users.updated(customerId, currentUserData.withEmail(email)))
      case None                  => state
    }

  }

  case class PasswordChanged(
      override val customerId: CustomerId,
      encryptedPassword: String,
      override val instant: Instant = Instant.now()
  ) extends UsersManagerEvent {
    override def applyTo(state: UsersManagerState): UsersManagerState = state.users.get(customerId) match {
      case Some(currentUserData) =>
        state.copy(users = state.users.updated(customerId, currentUserData.withEncryptedPassword(encryptedPassword)))
      case None => state
    }

  }

  // -----
  case class LoginUserWithEmail(email: Email, encryptedPassword: String)(val replyTo: ActorRef[StatusReply[CustomerId]])
      extends UsersManagerCommand {
    def validateAndGetCustomerId(state: UsersManagerState): ValidatedNel[String, CustomerId] =
      email
        .validate()
        .andThen(_ => validateEncryptedPassword(encryptedPassword))
        .andThen(_ => customerEmailExistsValidation(state, email))
        .andThen { case (customerId, userData) =>
          condNel(userData.encryptedPassword == encryptedPassword, customerId, "Wrong credentials")
        }

    override def validate(state: UsersManagerState): ValidatedNel[String, Unit] =
      validateAndGetCustomerId(state).map(_ => ())
  }

  // -----
  case class GetCustomerUserData(customerId: CustomerId)(val replyTo: ActorRef[StatusReply[UserData]])
      extends UsersManagerCommand {
    def validateAndGetUserData(state: UsersManagerState): ValidatedNel[String, UserData] =
      customerIdExistsValidation(state, customerId)

    override def validate(state: UsersManagerState): ValidatedNel[String, Unit] =
      validateAndGetUserData(state).map(_ => ())
  }

  // -----
  case class GetCustomerId(email: Email)(val replyTo: ActorRef[StatusReply[CustomerId]]) extends UsersManagerCommand {
    def validateAndGetCustomerId(state: UsersManagerState): ValidatedNel[String, CustomerId] =
      email
        .validate()
        .andThen(_ => customerEmailExistsValidation(state, email))
        .map(_._1)

    override def validate(state: UsersManagerState): ValidatedNel[String, Unit] =
      validateAndGetCustomerId(state).map(_ => ())
  }

}
