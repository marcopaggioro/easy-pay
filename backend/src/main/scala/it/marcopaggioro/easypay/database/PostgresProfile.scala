package it.marcopaggioro.easypay.database

import com.github.tminglei.slickpg._
import it.marcopaggioro.easypay.domain.classes.Money
import it.marcopaggioro.easypay.domain.classes.userdata.{
  CustomerFirstName,
  CustomerFullName,
  CustomerLastName,
  Email,
  EncryptedPassword
}
import slick.jdbc.JdbcType

import java.time.{Instant, LocalDate, YearMonth}

trait PostgresProfile
    extends ExPostgresProfile
    with PgArraySupport
    with PgDate2Support
    with PgRangeSupport
    with PgHStoreSupport
    with PgSearchSupport
    with PgNetSupport
    with PgLTreeSupport {
  def pgjson = "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[slick.basic.Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  override val api = MyAPI

  object MyAPI
      extends ExtPostgresAPI
      with ArrayImplicits
      with Date2DateTimeImplicitsDuration
      with NetImplicits
      with LTreeImplicits
      with RangeImplicits
      with HStoreImplicits
      with SearchImplicits
      with SearchAssistants {
    implicit val strListTypeMapper: DriverJdbcType[List[String]] = new SimpleArrayJdbcType[String]("text").to(_.toList)
  }
}

object PostgresProfile extends PostgresProfile {
  import it.marcopaggioro.easypay.database.PostgresProfile.MyAPI.*

  implicit val CustomerFirstNameMapper: JdbcType[CustomerFirstName] = MappedColumnType.base[CustomerFirstName, String](
    firstName => firstName.value,
    value => CustomerFirstName(value)
  )

  implicit val CustomerLastNameMapper: JdbcType[CustomerLastName] = MappedColumnType.base[CustomerLastName, String](
    lastName => lastName.value,
    value => CustomerLastName(value)
  )

  implicit val CustomerFullNameMapper: JdbcType[CustomerFullName] = MappedColumnType.base[CustomerFullName, String](
    fullName => fullName.value,
    value => CustomerFullName(value)
  )

  implicit val MoneyMapper: JdbcType[Money] = MappedColumnType.base[Money, BigDecimal](
    money => money.value,
    bigDecimal => Money(bigDecimal)
  )

  implicit val EmailMapper: JdbcType[Email] = MappedColumnType.base[Email, String](
    email => email.value,
    value => Email(value)
  )

  implicit val InstantMapper: JdbcType[Instant] = MappedColumnType.base[Instant, Long](
    instant => instant.toEpochMilli,
    milliSeconds => Instant.ofEpochMilli(milliSeconds)
  )

  implicit val EncryptedPasswordMapper: JdbcType[EncryptedPassword] = MappedColumnType.base[EncryptedPassword, String](
    encryptedPassword => encryptedPassword.value,
    value => EncryptedPassword(value)
  )

  implicit val YearMonthColumnMapper: JdbcType[YearMonth] = MappedColumnType.base[YearMonth, LocalDate](
    yearMonth => yearMonth.atDay(1),
    localDate => YearMonth.from(localDate)
  )

}
