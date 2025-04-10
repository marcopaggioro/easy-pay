import Decimal from 'decimal.js';

export interface Operation {
  transactionId: string;
  recipientCustomerId?: string;
  instant: Date;
  amount: Decimal;
}
