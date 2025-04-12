package it.marcopaggioro.easypay.database.users

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.Money
import it.marcopaggioro.easypay.domain.classes.userdata.{CustomerFirstName, CustomerLastName, Email}

import java.time.LocalDate

case class UserRecord(
    customerId: CustomerId,
    firstName: CustomerFirstName,
    lastName: CustomerLastName,
    birthDate: LocalDate,
    email: Email,
)

object UserRecord {

  implicit val UserRecordEncoder: Encoder[UserRecord] = Encoder.instance { userRecord =>
    Json.obj(
      "id" -> userRecord.customerId.asJson,
      "firstName" -> userRecord.firstName.asJson,
      "lastName" -> userRecord.lastName.asJson,
      "birthDate" -> userRecord.birthDate.asJson,
      "email" -> userRecord.email.asJson
    )
  }
  val UserRecordInteractedEncoder: Encoder[UserRecord] = Encoder.instance { recipientUserRecord =>
    Json.obj(
      "interactedFirstName" -> recipientUserRecord.firstName.asJson,
      "interactedLastName" -> recipientUserRecord.lastName.asJson,
      "interactedEmail" -> recipientUserRecord.email.asJson
    )
  }

}
