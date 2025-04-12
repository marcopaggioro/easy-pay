package it.marcopaggioro.easypay.routes.payloads

import cats.data.{Validated, ValidatedNel}
import cats.implicits.toTraverseOps
import cats.syntax.validated.*
import io.circe.Decoder
import io.circe.generic.auto.deriveDecoder
import it.marcopaggioro.easypay.domain.classes.Validable
import it.marcopaggioro.easypay.domain.classes.userdata.{CustomerFirstName, CustomerLastName, Email, EncryptedPassword}
import it.marcopaggioro.easypay.utilities.ValidationUtilities.validateBirthDate

import java.time.LocalDate

case class UpdateUserDataPayload(
    maybeFirstName: Option[CustomerFirstName],
    maybeLastName: Option[CustomerLastName],
    maybeBirthDate: Option[LocalDate],
    maybeEmail: Option[Email],
    maybeEncryptedPassword: Option[EncryptedPassword]
) extends Validable[UpdateUserDataPayload] {
  override def validate(): ValidatedNel[String, UpdateUserDataPayload] =
    maybeEmail
      .traverse(_.validate())
      .andThen(_ => maybeFirstName.traverse(_.validate()))
      .andThen(_ => maybeLastName.traverse(_.validate()))
      .andThen(_ => maybeBirthDate.traverse(validateBirthDate))
      .andThen(_ => maybeEmail.traverse(_.validate()))
      .andThen(_ => maybeEncryptedPassword.traverse(_.validate()))
      .map(_ => this)
}

object UpdateUserDataPayload {

  implicit val UpdateUserDataPayloadDecoder: Decoder[UpdateUserDataPayload] = Decoder.instance { cursor =>
    for {
      maybeFirstName <- cursor.get[Option[CustomerFirstName]]("firstName")
      maybeLastName <- cursor.get[Option[CustomerLastName]]("lastName")
      maybeBirthDate <- cursor.get[Option[LocalDate]]("birthDate")
      maybeEmail <- cursor.get[Option[Email]]("email")
      maybeEncryptedPassword <- cursor.get[Option[EncryptedPassword]]("encryptedPassword")
    } yield UpdateUserDataPayload(maybeFirstName, maybeLastName, maybeBirthDate, maybeEmail, maybeEncryptedPassword)
  }

}
