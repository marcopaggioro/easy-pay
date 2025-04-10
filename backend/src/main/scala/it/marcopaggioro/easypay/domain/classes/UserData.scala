package it.marcopaggioro.easypay.domain.classes

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerName, CustomerSurname, EncryptedPassword}
import it.marcopaggioro.easypay.routes.payloads.UpdateUserDataPayload
import it.marcopaggioro.easypay.utilities.ValidationUtilities.{
  validateBirthDate,
  validateCustomerName,
  validateCustomerSurname,
  validateEncryptedPassword
}

import java.time.{LocalDate, Period}

case class UserData private (
    name: CustomerName,
    surname: CustomerSurname,
    birthDate: LocalDate,
    email: Email,
    encryptedPassword: EncryptedPassword
) extends Validable[UserData] {

  override def validate(): ValidatedNel[String, UserData] = email
    .validate()
    .andThen(_ => validateCustomerName(name))
    .andThen(_ => validateCustomerSurname(surname))
    .andThen(_ => validateEncryptedPassword(encryptedPassword))
    .andThen(_ => validateBirthDate(birthDate))
    .map(_ => this)

  def withEmail(email: Email): UserData = copy(email = email)
  def withName(name: CustomerName): UserData = copy(name = name.trim)
  def withSurname(surname: CustomerSurname): UserData = copy(surname = surname.trim)
  def withEncryptedPassword(encryptedPassword: EncryptedPassword): UserData = copy(encryptedPassword = encryptedPassword.trim)
}

object UserData {

  def apply(
      name: CustomerName,
      surname: CustomerSurname,
      birthDate: LocalDate,
      email: Email,
      encryptedPassword: EncryptedPassword
  ) =
    new UserData(name.trim.capitalize, surname.trim.capitalize, birthDate, email, encryptedPassword.trim)

  implicit val UserDataEncoder: Encoder[UserData] = deriveEncoder[UserData]
  implicit val UserDataDecoder: Decoder[UserData] = Decoder.instance { cursor =>
    for {
      name <- cursor.get[CustomerName]("name")
      surname <- cursor.get[CustomerSurname]("surname")
      birthDate <- cursor.get[LocalDate]("birthDate")
      email <- cursor.get[Email]("email")
      encryptedPassword <- cursor.get[EncryptedPassword]("encryptedPassword")
    } yield UserData(name, surname, birthDate, email, encryptedPassword)
  }

}
