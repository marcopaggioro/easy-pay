package it.marcopaggioro.easypay.database.usersbalance

import it.marcopaggioro.easypay.database.PostgresProfile.*
import it.marcopaggioro.easypay.database.PostgresProfile.api.*
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.Money
import it.marcopaggioro.easypay.domain.classes.userdata.{CustomerFirstName, CustomerLastName, Email}
import slick.jdbc.H2Profile.Table
import slick.lifted.{ProvenShape, Rep, TableQuery, Tag}

import java.time.LocalDate

class UsersBalanceTable(tag: Tag) extends Table[UserBalanceRecord](tag, "users_balance") {

  def customerId: Rep[CustomerId] = column[CustomerId]("customer_id")
  def balance: Rep[Money] = column[Money]("balance")

  override def * : ProvenShape[UserBalanceRecord] = (customerId, balance).mapTo[UserBalanceRecord]

  def pk = primaryKey("pk_users_balance", customerId)

}

object UsersBalanceTable {

  lazy val Table: TableQuery[UsersBalanceTable] = TableQuery[UsersBalanceTable]

}
