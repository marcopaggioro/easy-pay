package it.marcopaggioro.easypay.utilities

import cats.data.Validated.condNel
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import it.marcopaggioro.easypay.domain.classes.Aliases.{CustomerId}
import it.marcopaggioro.easypay.domain.classes.Money

import java.time.{LocalDate, LocalDateTime, Period}

object ValidationUtilities {

  private lazy val minAge: Int = 16
  def validateBirthDate(date: LocalDate): ValidatedNel[String, Unit] =
    condNel(date.isBefore(LocalDate.now()), (), "Birth date can not be in future")
      .andThen(_ => condNel(Period.between(date, LocalDate.now()).getYears >= minAge, (), s"Required minimum of $minAge years"))

  def validatePositiveAmount(amount: Money): ValidatedNel[String, Unit] = condNel(amount.value > 0, (), "Amount must be more than 0")

  def validateDateTimeInFuture(dateTime: LocalDateTime): ValidatedNel[String, Unit] =
    condNel(dateTime.isAfter(LocalDateTime.now()), (), "Date must be in future")

  private lazy val minPeriod: Period = Period.ofDays(1)
  def validateMinimumPeriod(period: Period): ValidatedNel[String, Unit] =
    condNel(LocalDate.now().plus(period).isAfter(LocalDate.now().plus(minPeriod)), (), s"Minimum period is $minPeriod")

  def differentCustomerIdsValidation(first: CustomerId, second: CustomerId): ValidatedNel[String, Unit] =
    condNel(first != second, (), "Can not operate on yourself")

}
