import Decimal from 'decimal.js';

export interface WalletOperation {
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
