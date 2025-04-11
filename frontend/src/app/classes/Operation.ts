import Decimal from 'decimal.js';

export interface Operation {
  transactionId: string;
  senderCustomerId: string;
  recipientCustomerId: string;
  interactedFirstName: string;
  interactedLastName: string;
  interactedEmail: string;
  instant: Date;
  amount: Decimal;
}
