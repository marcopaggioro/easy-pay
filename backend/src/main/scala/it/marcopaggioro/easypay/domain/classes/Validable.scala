package it.marcopaggioro.easypay.domain.classes

import cats.data.ValidatedNel

trait Validable[R] {

  def validate(): ValidatedNel[String, R]

}
