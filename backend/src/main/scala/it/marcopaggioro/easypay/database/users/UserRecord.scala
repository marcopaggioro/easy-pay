package it.marcopaggioro.easypay.database.users

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.userdata.{CustomerFirstName, CustomerLastName, Email, EncryptedPassword}

import java.time.{Instant, LocalDate}

case class UserRecord(
    customerId: CustomerId,
    firstName: CustomerFirstName,
    lastName: CustomerLastName,
    birthDate: LocalDate,
    email: Email,
    encryptedPassword: EncryptedPassword,
    lastEdit: Instant
)

object UserRecord {

  implicit val UserRecordEncoder: Encoder[UserRecord] = Encoder.instance { userRecord =>
    Json.obj(
      "id" -> userRecord.customerId.asJson,
      "firstName" -> userRecord.firstName.asJson,
      "lastName" -> userRecord.lastName.asJson,
      "birthDate" -> userRecord.birthDate.asJson,
      "email" -> userRecord.email.asJson,
      "lastEdit" -> userRecord.lastEdit.toEpochMilli.asJson
    )
  }
  val UserRecordInteractedEncoder: Encoder[UserRecord] = Encoder.instance { recipientUserRecord =>
    Json.obj(
      "interactedCustomerId" -> recipientUserRecord.customerId.asJson,
      "interactedFirstName" -> recipientUserRecord.firstName.asJson,
      "interactedLastName" -> recipientUserRecord.lastName.asJson,
      "interactedEmail" -> recipientUserRecord.email.asJson
    )
  }

}
