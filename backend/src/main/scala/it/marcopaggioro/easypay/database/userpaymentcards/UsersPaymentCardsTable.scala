package it.marcopaggioro.easypay.database.userpaymentcards

import it.marcopaggioro.easypay.database.PostgresProfile._
import it.marcopaggioro.easypay.database.PostgresProfile.api._
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.userdata.CustomerFullName
import slick.jdbc.H2Profile.Table
import slick.lifted.{Rep, TableQuery, Tag, _}

import java.util.UUID

class UsersPaymentCardsTable(tag: Tag) extends Table[UserPaymentCardRecord](tag, "users_payment_cards") {

  def customerId: Rep[CustomerId] = column[CustomerId]("customer_id")
  def cardId: Rep[Int] = column[Int]("card_id")
  def fullName: Rep[CustomerFullName] = column[CustomerFullName]("full_name")
  def cardNumber: Rep[String] = column[String]("card_number")
  def expiration: Rep[String] = column[String]("expiration")

  override def * : ProvenShape[UserPaymentCardRecord] =
    (customerId, cardId, fullName, cardNumber, expiration).mapTo[UserPaymentCardRecord]

  def pk = primaryKey("pk_users_payment_cards", (customerId, cardId))

}

object UsersPaymentCardsTable {

  lazy val Table: TableQuery[UsersPaymentCardsTable] = TableQuery[UsersPaymentCardsTable]

}
