package it.marcopaggioro.easypay.domain.classes

import java.util.UUID

object Aliases {

  type CustomerName = String
  type CustomerSurname = String
  type EncryptedPassword = String

  type CustomerId = UUID
  type TransactionId = UUID
  type ScheduledOperationId = UUID

}
