package it.marcopaggioro.easypay.domain.classes

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import io.circe.{Encoder, Json}
import it.marcopaggioro.easypay.domain.classes.Status.{Done, Failed, Pending}

import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[Pending], name = "pending"),
    new JsonSubTypes.Type(value = classOf[Done], name = "done"),
    new JsonSubTypes.Type(value = classOf[Failed], name = "failed")
  )
)
sealed trait Status {
  val instant: Instant
}

object Status {

  case class Pending(override val instant: Instant = Instant.now()) extends Status
  case class Done(override val instant: Instant = Instant.now()) extends Status
  case class Failed(error: String, override val instant: Instant = Instant.now()) extends Status

  implicit val StatusEncoder: Encoder[Status] = Encoder.instance {
    case _: Pending => Json.fromString("pending")
    case _: Done    => Json.fromString("done")
    case _: Failed  => Json.fromString("failed")
  }

}
