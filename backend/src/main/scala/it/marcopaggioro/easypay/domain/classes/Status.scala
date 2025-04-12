package it.marcopaggioro.easypay.domain.classes

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import io.circe.Encoder
import io.circe.syntax.EncoderOps
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
  val code: String
  val instant: Instant
}

object Status {

  case class Pending(override val instant: Instant = Instant.now()) extends Status {
    override val code: String = "In attesa"
  }
  case class Done(override val instant: Instant = Instant.now()) extends Status {
    override val code: String = "Completata"
  }
  case class Failed(error: String, override val instant: Instant = Instant.now()) extends Status {
    override val code: String = "Fallita"
  }

  implicit val StatusEncoder: Encoder[Status] = Encoder.instance { status =>
    status.code.asJson
  }

}
