package it.marcopaggioro.easypay.domain.classes

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerName, CustomerSurname, EncryptedPassword}
import it.marcopaggioro.easypay.utilities.ValidationUtilities.{
  validateBirthDate,
  validateCustomerName,
  validateCustomerSurname,
  validateEncryptedPassword
}

import java.time.{LocalDate, Period}

case class UserData private (
    email: Email,
    name: CustomerName,
    surname: CustomerSurname,
    encryptedPassword: EncryptedPassword,
    birthDate: LocalDate
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
      email: Email,
      name: CustomerName,
      surname: CustomerSurname,
      encryptedPassword: EncryptedPassword,
      birthDate: LocalDate
  ) =
    new UserData(email, name.trim, surname.trim, encryptedPassword.trim, birthDate)

  implicit val UserDataEncoder: Encoder[UserData] = deriveEncoder[UserData]
  implicit val UserDataDecoder: Decoder[UserData] = deriveDecoder[UserData]

}
