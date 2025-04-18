package it.marcopaggioro.easypay.database.usersbalance

import it.marcopaggioro.easypay.database.PostgresProfile._
import it.marcopaggioro.easypay.database.PostgresProfile.api._
import it.marcopaggioro.easypay.database.users.UsersTable
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.Money
import slick.jdbc.H2Profile.Table
import slick.lifted.{ProvenShape, Rep, TableQuery, Tag}

class UsersBalanceTable(tag: Tag) extends Table[UserBalanceRecord](tag, "users_balance") {

  def customerId: Rep[CustomerId] = column[CustomerId]("customer_id")
  def balance: Rep[Money] = column[Money]("balance")

  override def * : ProvenShape[UserBalanceRecord] = (customerId, balance).mapTo[UserBalanceRecord]

  def pk = primaryKey("pk_users_balance", customerId)

  def customerIdFk = foreignKey("fk_users_balance", customerId, UsersTable.Table)(_.customerId)

}

object UsersBalanceTable {

  lazy val Table: TableQuery[UsersBalanceTable] = TableQuery[UsersBalanceTable]

}
