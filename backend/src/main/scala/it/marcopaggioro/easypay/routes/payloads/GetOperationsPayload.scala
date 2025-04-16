package it.marcopaggioro.easypay.routes.payloads

import cats.data.Validated.condNel
import cats.data.ValidatedNel
import cats.implicits.toTraverseOps
import io.circe.Decoder
import it.marcopaggioro.easypay.domain.classes.Validable
import it.marcopaggioro.easypay.domain.classes.userdata.Email

import java.time.Instant

case class GetOperationsPayload(
    page: Int,
    maybeEmail: Option[Email],
    maybeFullName: Option[String],
    maybeStartDate: Option[Instant],
    maybeEndDate: Option[Instant]
) extends Validable[GetOperationsPayload] {
  override def validate(): ValidatedNel[String, GetOperationsPayload] = condNel(
    page > 0,
    (),
    "Il numero della pagina deve essere maggiore di 0"
  ).andThen(_ => maybeEmail.traverse(_.validate()))
    .andThen(_ =>
      maybeFullName.traverse(fullName => condNel(fullName.trim.nonEmpty, (), "Il nome completo non può essere vuoto"))
    )
    .andThen(_ =>
      maybeStartDate.traverse(startDate =>
        condNel(startDate.isBefore(Instant.now()), (), "La data di inizio non può essere nel futuro").andThen(_ =>
          maybeEndDate.traverse(endDate =>
            condNel(startDate.isBefore(endDate), (), "La data di fine non può essere antecedente la data di inizio")
          )
        )
      )
    )
    .map(_ => this)
}

object GetOperationsPayload {

  implicit val GetOperationsPayloadDecoder: Decoder[GetOperationsPayload] = Decoder.instance { cursor =>
    for {
      maybePage <- cursor.get[Option[Int]]("page")
      maybeEmail <- cursor.get[Option[Email]]("email")
      maybeFullName <- cursor.get[Option[String]]("fullName")
      maybeStartDate <- cursor.get[Option[Instant]]("start")
      maybeEndDate <- cursor.get[Option[Instant]]("end")
    } yield GetOperationsPayload(maybePage.getOrElse(1), maybeEmail, maybeFullName, maybeStartDate, maybeEndDate)
  }

}
