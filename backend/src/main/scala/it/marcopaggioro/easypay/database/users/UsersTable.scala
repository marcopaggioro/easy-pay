package it.marcopaggioro.easypay.database.users

import it.marcopaggioro.easypay.database.PostgresProfile._
import it.marcopaggioro.easypay.database.PostgresProfile.api._
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.userdata.{CustomerFirstName, CustomerLastName, Email, EncryptedPassword}
import slick.jdbc.H2Profile.Table
import slick.lifted.{ProvenShape, Rep, TableQuery, Tag}

import java.time.{Instant, LocalDate}

class UsersTable(tag: Tag) extends Table[UserRecord](tag, "users") {

  def customerId: Rep[CustomerId] = column[CustomerId]("customer_id")
  def firstName: Rep[CustomerFirstName] = column[CustomerFirstName]("first_name", O.Length(CustomerFirstName.maxLength))
  def lastName: Rep[CustomerLastName] = column[CustomerLastName]("last_name", O.Length(CustomerLastName.maxLength))
  def birtDate: Rep[LocalDate] = column[LocalDate]("birth_date")
  def email: Rep[Email] = column[Email]("email", O.Unique, O.Length(Email.maxLength))
  def encryptedPassword: Rep[EncryptedPassword] =
    column[EncryptedPassword]("encrypted_password", O.Length(EncryptedPassword.maxLength))
  def lastEdit: Rep[Instant] = column[Instant]("last_edit")(InstantMapper)

  override def * : ProvenShape[UserRecord] =
    (customerId, firstName, lastName, birtDate, email, encryptedPassword, lastEdit).mapTo[UserRecord]

  def pk = primaryKey("pk_users", customerId)

}

object UsersTable {

  lazy val Table: TableQuery[UsersTable] = TableQuery[UsersTable]

}
