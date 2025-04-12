package it.marcopaggioro.easypay.database.usersbalance

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.Money
import it.marcopaggioro.easypay.domain.classes.userdata.{CustomerFirstName, CustomerLastName, Email}

import java.time.LocalDate

case class UserBalanceRecord(
    customerId: CustomerId,
    balance: Money
)

object UserBalanceRecord {

  implicit val UserBalanceRecordEncoder: Encoder[UserBalanceRecord] = Encoder.instance { userBalanceRecord =>
    Json.obj(
      "id" -> userBalanceRecord.customerId.asJson,
      "balance" -> userBalanceRecord.balance.asJson
    )
  }

}
