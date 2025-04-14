package it.marcopaggioro.easypay.utilities

import cats.data.Validated.condNel
import cats.data.{Validated, ValidatedNel}
import it.marcopaggioro.easypay.AppConfig
import it.marcopaggioro.easypay.domain.classes.Aliases.CustomerId
import it.marcopaggioro.easypay.domain.classes.Money

import java.time.{Instant, LocalDate, Period}

object ValidationUtilities {

  lazy val GenericError: String = "Errore generico"

  private lazy val maxDescriptionLength: Int = 500
  def validateDescription(value: String): ValidatedNel[String, Unit] =
    condNel(value.trim.nonEmpty, (), "La descrizione non può essere vuota").andThen(_ =>
      condNel(
        value.trim.length <= maxDescriptionLength,
        (),
        s"La descrizione può essere lunga al massimo $maxDescriptionLength caratteri"
      )
    )

  private lazy val minAge: Int = 16
  def validateBirthDate(date: LocalDate): ValidatedNel[String, Unit] =
    condNel(date.isBefore(LocalDate.now()), (), "La data di nascita non può essere nel futuro")
      .andThen(_ =>
        condNel(Period.between(date, LocalDate.now()).getYears >= minAge, (), s"E' richiesta un'età minima di $minAge anni")
      )

  def validatePositiveAmount(amount: Money): ValidatedNel[String, Unit] =
    condNel(amount.value > 0, (), "L'importo deve essere maggiore di 0")

  def validateInstantInFuture(instant: Instant): ValidatedNel[String, Unit] =
    condNel(instant.isAfter(Instant.now()), (), "La data deve essere nel futuro")

  def validateMinimumPeriod(period: Period): ValidatedNel[String, Unit] =
    condNel(
      !LocalDate.now().plus(period).isBefore(LocalDate.now().plus(AppConfig.minScheduledOperationPeriod)),
      (),
      s"Il periodo minimo è $AppConfig.minScheduledOperationPeriod"
    )

  def differentCustomerIdsValidation(first: CustomerId, second: CustomerId): ValidatedNel[String, Unit] =
    condNel(first != second, (), "Non puoi operare su te stesso")

}
