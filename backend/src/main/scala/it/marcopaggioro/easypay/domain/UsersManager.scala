package it.marcopaggioro.easypay.domain

import akka.Done
import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import cats.data.Validated.{Invalid, Valid, condNel, invalidNel, validNel}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.implicits.toTraverseOps
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.Domain.{DomainCommand, DomainEvent, DomainState}
import it.marcopaggioro.easypay.domain.classes.userdata.{CustomerFirstName, CustomerLastName, Email, EncryptedPassword, UserData}
import it.marcopaggioro.easypay.utilities.ValidationUtilities.validateBirthDate

import java.time.{Instant, LocalDate}
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
  case class CreateUser(customerId: CustomerId, userData: UserData)(val replyTo: ActorRef[StatusReply[CustomerId]])
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
      maybeFirstName: Option[CustomerFirstName],
      maybeLastName: Option[CustomerLastName],
      maybeBirthDate: Option[LocalDate],
      maybeEmail: Option[Email],
      maybeEncryptedPassword: Option[EncryptedPassword]
  )(val replyTo: ActorRef[StatusReply[Done]])
      extends UsersManagerCommand {
    override def validate(state: UsersManagerState): ValidatedNel[String, Unit] =
      customerIdExistsValidation(state, customerId)
        .andThen(_ => maybeFirstName.traverse(_.validate()))
        .andThen(_ => maybeLastName.traverse(_.validate()))
        .andThen(_ => maybeBirthDate.traverse(validateBirthDate))
        .andThen(_ => maybeEmail.traverse(email => email.validate().andThen(_ => emailNotAlreadyExistsValidation(state, email))))
        .andThen(_ => maybeEncryptedPassword.traverse(_.validate()))
        .map(_ => ())

    override protected def generateEvents(state: UsersManagerState): List[UsersManagerEvent] =
      state.users.get(customerId) match {
        case None =>
          List.empty

        case Some(userData) =>
          val firstNameEvent: Option[FirstNameChanged] =
            maybeFirstName.flatMap(firstName =>
              Option.when(userData.firstName != firstName)(FirstNameChanged(customerId, firstName))
            )
          val lastNameEvent: Option[LastNameChanged] =
            maybeLastName.flatMap(lastName => Option.when(userData.lastName != lastName)(LastNameChanged(customerId, lastName)))
          val birthDateEvent: Option[BirthDateChanged] =
            maybeBirthDate.flatMap(birthDate =>
              Option.when(userData.birthDate != birthDate)(BirthDateChanged(customerId, birthDate))
            )
          val emailEvent: Option[EmailChanged] =
            maybeEmail.flatMap(email => Option.when(userData.email != email)(EmailChanged(customerId, email)))
          val passwordEvent: Option[PasswordChanged] =
            maybeEncryptedPassword.flatMap(encryptedPassword =>
              Option.when(userData.encryptedPassword != encryptedPassword)(
                PasswordChanged(customerId, encryptedPassword)
              )
            )

          List(firstNameEvent, lastNameEvent, birthDateEvent, emailEvent, passwordEvent).flatten
      }
  }

  case class FirstNameChanged(
      override val customerId: CustomerId,
      firstName: CustomerFirstName,
      override val instant: Instant = Instant.now()
  ) extends UsersManagerEvent {
    override def applyTo(state: UsersManagerState): UsersManagerState = state.users.get(customerId) match {
      case Some(currentUserData) =>
        state.copy(users = state.users.updated(customerId, currentUserData.copy(firstName = firstName)))
      case None => state
    }
  }

  case class LastNameChanged(
      override val customerId: CustomerId,
      lastName: CustomerLastName,
      override val instant: Instant = Instant.now()
  ) extends UsersManagerEvent {
    override def applyTo(state: UsersManagerState): UsersManagerState = state.users.get(customerId) match {
      case Some(currentUserData) => state.copy(users = state.users.updated(customerId, currentUserData.copy(lastName = lastName)))
      case None                  => state
    }
  }

  case class BirthDateChanged(
      override val customerId: CustomerId,
      birthDate: LocalDate,
      override val instant: Instant = Instant.now()
  ) extends UsersManagerEvent {
    override def applyTo(state: UsersManagerState): UsersManagerState = state.users.get(customerId) match {
      case Some(currentUserData) =>
        state.copy(users = state.users.updated(customerId, currentUserData.copy(birthDate = birthDate)))
      case None => state
    }
  }

  case class EmailChanged(override val customerId: CustomerId, email: Email, override val instant: Instant = Instant.now())
      extends UsersManagerEvent {
    override def applyTo(state: UsersManagerState): UsersManagerState = state.users.get(customerId) match {
      case Some(currentUserData) => state.copy(users = state.users.updated(customerId, currentUserData.copy(email = email)))
      case None                  => state
    }

  }

  case class PasswordChanged(
      override val customerId: CustomerId,
      encryptedPassword: EncryptedPassword,
      override val instant: Instant = Instant.now()
  ) extends UsersManagerEvent {
    override def applyTo(state: UsersManagerState): UsersManagerState = state.users.get(customerId) match {
      case Some(currentUserData) =>
        state.copy(users = state.users.updated(customerId, currentUserData.copy(encryptedPassword = encryptedPassword)))
      case None => state
    }

  }

  // -----
  case class LoginUserWithEmail(email: Email, encryptedPassword: EncryptedPassword)(
      val replyTo: ActorRef[StatusReply[CustomerId]]
  ) extends UsersManagerCommand {
    def validateAndGetCustomerId(state: UsersManagerState): ValidatedNel[String, CustomerId] =
      email
        .validate()
        .andThen(_ => encryptedPassword.validate())
        .andThen(_ => customerEmailExistsValidation(state, email))
        .andThen { case (customerId, userData) =>
          condNel(userData.encryptedPassword == encryptedPassword, customerId, "Wrong credentials")
        }

    override def validate(state: UsersManagerState): ValidatedNel[String, Unit] =
      validateAndGetCustomerId(state).map(_ => ())
  }

}
