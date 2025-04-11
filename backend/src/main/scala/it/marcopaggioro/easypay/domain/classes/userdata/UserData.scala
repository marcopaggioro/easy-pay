package it.marcopaggioro.easypay.domain.classes.userdata

import cats.data.ValidatedNel
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}
import it.marcopaggioro.easypay.domain.classes.Validable
import it.marcopaggioro.easypay.utilities.ValidationUtilities.validateBirthDate

import java.time.LocalDate

case class UserData(
    firstName: CustomerFirstName,
    lastName: CustomerLastName,
    birthDate: LocalDate,
    email: Email,
    encryptedPassword: EncryptedPassword
) extends Validable[UserData] {
  override def validate(): ValidatedNel[String, UserData] = email
    .validate()
    .andThen(_ => firstName.validate())
    .andThen(_ => lastName.validate())
    .andThen(_ => encryptedPassword.validate())
    .andThen(_ => validateBirthDate(birthDate))
    .map(_ => this)
}

object UserData {

  implicit val UserDataEncoder: Encoder[UserData] = Encoder.instance { userData =>
    Json.obj(
      "firstName" -> userData.firstName.asJson,
      "lastName" -> userData.lastName.asJson,
      "birthDate" -> userData.birthDate.asJson,
      "email" -> userData.email.asJson,
      "encryptedPassword" -> userData.encryptedPassword.asJson
    )
  }
  implicit val UserDataDecoder: Decoder[UserData] = Decoder.instance { cursor =>
    for {
      firstName <- cursor.get[CustomerFirstName]("firstName")
      lastName <- cursor.get[CustomerLastName]("lastName")
      birthDate <- cursor.get[LocalDate]("birthDate")
      email <- cursor.get[Email]("email")
      encryptedPassword <- cursor.get[EncryptedPassword]("encryptedPassword")
    } yield UserData(firstName, lastName, birthDate, email, encryptedPassword)
  }

}
