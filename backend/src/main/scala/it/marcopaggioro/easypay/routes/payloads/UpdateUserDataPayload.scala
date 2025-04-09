package it.marcopaggioro.easypay.routes.payloads

import cats.data.ValidatedNel
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerName, CustomerSurname, EncryptedPassword}
import it.marcopaggioro.easypay.domain.classes.{Email, Validable}
import cats.data.ValidatedNel
import cats.data.Validated
import cats.implicits.toTraverseOps
import cats.syntax.validated.*
import it.marcopaggioro.easypay.utilities.ValidationUtilities.{
  validateBirthDate,
  validateCustomerName,
  validateCustomerSurname,
  validateEncryptedPassword
}

case class UpdateUserDataPayload(
    maybeEmail: Option[Email],
    maybeName: Option[CustomerName],
    maybeSurname: Option[CustomerSurname],
    maybeEncryptedPassword: Option[EncryptedPassword]
) extends Validable[UpdateUserDataPayload] {
  override def validate(): ValidatedNel[String, UpdateUserDataPayload] =
    maybeEmail
      .traverse(_.validate())
      .andThen(_ => maybeName.traverse(validateCustomerName))
      .andThen(_ => maybeSurname.traverse(validateCustomerSurname))
      .andThen(_ => maybeEncryptedPassword.traverse(validateEncryptedPassword))
      .map(_ => this)
}

object UpdateUserDataPayload {

  implicit val UpdateUserDataPayloadDecoder: Decoder[UpdateUserDataPayload] = Decoder.instance { cursor =>
    for {
      maybeEmail <- cursor.get[Option[Email]]("email")
      maybeName <- cursor.get[Option[CustomerName]]("name")
      maybeSurname <- cursor.get[Option[CustomerSurname]]("surname")
      maybeEncryptedPassword <- cursor.get[Option[EncryptedPassword]]("encryptedPassword")
    } yield UpdateUserDataPayload(maybeEmail, maybeName, maybeSurname, maybeEncryptedPassword)
  }

}
