package it.marcopaggioro.easypay.database.users

import it.marcopaggioro.easypay.database.PostgresProfile.api.*
import it.marcopaggioro.easypay.database.PostgresProfile._
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerId, TransactionId}
import it.marcopaggioro.easypay.domain.classes.Money
import it.marcopaggioro.easypay.domain.classes.userdata.{CustomerFirstName, CustomerLastName, Email}
import slick.ast.BaseTypedType
import slick.jdbc.H2Profile.Table
import slick.jdbc.JdbcType
import slick.lifted.{ProvenShape, Rep, TableQuery, Tag}

import java.time.{Instant, LocalDate}

class UsersTable(tag: Tag) extends Table[UserRecord](tag, "users") {

  def customerId: Rep[CustomerId] = column[CustomerId]("customer_id")
  def firstName: Rep[CustomerFirstName] = column[CustomerFirstName]("first_name")
  def lastName: Rep[CustomerLastName] = column[CustomerLastName]("last_name")
  def birtDate: Rep[LocalDate] = column[LocalDate]("birth_date")
  def email: Rep[Email] = column[Email]("email")

  override def * : ProvenShape[UserRecord] = (customerId, firstName, lastName, birtDate, email).mapTo[UserRecord]

  def pk = primaryKey("pk_users", customerId)

}

object UsersTable {

  lazy val Table: TableQuery[UsersTable] = TableQuery[UsersTable]

}
