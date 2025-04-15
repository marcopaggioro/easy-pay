import Decimal from 'decimal.js';

export interface Operation {
  transactionId: string;
  senderCustomerId: string;
  recipientCustomerId: string;
  description: string;
  interactedFirstName: string;
  interactedLastName: string;
  interactedEmail: string;
  instant: number;
  amount: Decimal;
}
