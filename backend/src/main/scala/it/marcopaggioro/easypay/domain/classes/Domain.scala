package it.marcopaggioro.easypay.domain.classes

import akka.serialization.jackson.CborSerializable
import cats.data.Validated.{Invalid, Valid, validNel}
import cats.data.ValidatedNel

import java.time.Instant

object Domain {

  trait DomainState extends CborSerializable

  trait DomainEvent[State <: DomainState] extends CborSerializable {
    val instant: Instant

    def applyTo(state: State): State
  }

  trait DomainCommand[State <: DomainState, Event <: DomainEvent[State]] extends CborSerializable {
    def validate(state: State): ValidatedNel[String, Unit]

    protected def generateEvents(state: State): List[Event] = List.empty[Event]

    def validateAndGenerateEvents(state: State): ValidatedNel[String, List[Event]] =
      validate(state) match {
        case Valid(_)             => validNel(generateEvents(state))
        case invalid @ Invalid(_) => invalid
      }
  }

}
