package it.marcopaggioro.easypay.domain

import akka.Done
import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import cats.data.Validated.{Invalid, Valid, condNel, invalidNel, validNel}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.implicits.toTraverseOps
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.Domain.{DomainCommand, DomainEvent, DomainState}
import it.marcopaggioro.easypay.domain.classes.userdata.paymentcard.PaymentCard
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
        case None           => invalidNel(s"Cliente $customerId non trovato")
      }

    def customerIdNotExistsValidation(state: UsersManagerState, customerId: CustomerId): ValidatedNel[String, Unit] =
      customerIdExistsValidation(state, customerId) match {
        case Valid(_)        => invalidNel(s"Cliente $customerId già esistente")
        case Invalid(errors) => validNel(())
      }

    def customerEmailExistsValidation(state: UsersManagerState, email: Email): ValidatedNel[String, (CustomerId, UserData)] =
      state.users
        .collectFirst {
          case (customerId, userData) if userData.email == email => validNel(customerId -> userData)
        }
        .getOrElse(invalidNel(s"Email ${email.value} non trovata"))

    def emailNotAlreadyExistsValidation(state: UsersManagerState, email: Email): ValidatedNel[String, Unit] =
      condNel(!state.usersEmails.contains(email), (), s"Email ${email.value} già esistente")

    def cardIdExistsValidation(state: UsersManagerState, customerId: CustomerId, cardId: Int): ValidatedNel[String, PaymentCard] =
      customerIdExistsValidation(state, customerId)
        .andThen(userData =>
          userData.paymentCards.get(cardId) match {
            case Some(paymentCard) =>
              validNel(paymentCard)

            case None =>
              invalidNel("Carta non esistente")
          }
        )
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
  case class AddPaymentCard(customerId: CustomerId, paymentCard: PaymentCard)(val replyTo: ActorRef[StatusReply[Done]])
      extends UsersManagerCommand {
    override def validate(state: UsersManagerState): ValidatedNel[String, Unit] =
      customerIdExistsValidation(state, customerId)
        .andThen(_ => paymentCard.validate())
        .map(_ => ())

    override protected def generateEvents(state: UsersManagerState): List[UsersManagerEvent] = {
      val cardId: Int = state.users.get(customerId) match {
        case Some(userData) =>
          userData.paymentCards.keys.maxOption.getOrElse(0) + 1

        case None => 0
      }
      List(
        PaymentCardAdded(customerId, cardId, paymentCard)
      )
    }
  }

  case class PaymentCardAdded(
      override val customerId: CustomerId,
      cardId: Int,
      paymentCard: PaymentCard,
      override val instant: Instant = Instant.now()
  ) extends UsersManagerEvent {
    override def applyTo(state: UsersManagerState): UsersManagerState = state.users.get(customerId) match {
      case Some(currentUserData) =>
        state.copy(users =
          state.users
            .updated(customerId, currentUserData.copy(paymentCards = currentUserData.paymentCards.updated(cardId, paymentCard)))
        )
      case None => state
    }
  }

  // -----
  case class DeletePaymentCard(customerId: CustomerId, cardId: Int)(val replyTo: ActorRef[StatusReply[Done]])
      extends UsersManagerCommand {
    override def validate(state: UsersManagerState): ValidatedNel[String, Unit] =
      cardIdExistsValidation(state, customerId, cardId).map(_ => ())

    override protected def generateEvents(state: UsersManagerState): List[UsersManagerEvent] = List(
      PaymentCardDeleted(customerId, cardId)
    )
  }

  case class PaymentCardDeleted(
      override val customerId: CustomerId,
      cardId: Int,
      override val instant: Instant = Instant.now()
  ) extends UsersManagerEvent {
    override def applyTo(state: UsersManagerState): UsersManagerState = state.users.get(customerId) match {
      case Some(currentUserData) =>
        state.copy(users =
          state.users.updated(customerId, currentUserData.copy(paymentCards = currentUserData.paymentCards.removed(cardId)))
        )
      case None => state
    }
  }

}
