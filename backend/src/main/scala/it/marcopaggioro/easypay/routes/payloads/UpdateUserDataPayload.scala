package it.marcopaggioro.easypay.routes.payloads

import cats.data.{Validated, ValidatedNel}
import cats.implicits.toTraverseOps
import cats.syntax.validated._
import io.circe.Decoder
import it.marcopaggioro.easypay.domain.classes.Validable
import it.marcopaggioro.easypay.domain.classes.userdata.{CustomerFirstName, CustomerLastName, Email, EncryptedPassword}

case class UpdateUserDataPayload(
    maybeEmail: Option[Email],
    maybeFirstName: Option[CustomerFirstName],
    maybeLastName: Option[CustomerLastName],
    maybeEncryptedPassword: Option[EncryptedPassword]
) extends Validable[UpdateUserDataPayload] {
  override def validate(): ValidatedNel[String, UpdateUserDataPayload] =
    maybeEmail
      .traverse(_.validate())
      .andThen(_ => maybeFirstName.traverse(_.validate()))
      .andThen(_ => maybeLastName.traverse(_.validate()))
      .andThen(_ => maybeEncryptedPassword.traverse(_.validate()))
      .map(_ => this)
}

object UpdateUserDataPayload {

  implicit val UpdateUserDataPayloadDecoder: Decoder[UpdateUserDataPayload] = Decoder.instance { cursor =>
    for {
      maybeEmail <- cursor.get[Option[Email]]("email")
      maybeFirstName <- cursor.get[Option[CustomerFirstName]]("firstName")
      maybeLastName <- cursor.get[Option[CustomerLastName]]("lastName")
      maybeEncryptedPassword <- cursor.get[Option[EncryptedPassword]]("encryptedPassword")
    } yield UpdateUserDataPayload(maybeEmail, maybeFirstName, maybeLastName, maybeEncryptedPassword)
  }

}
