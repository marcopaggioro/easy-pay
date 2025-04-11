import Decimal from 'decimal.js';

export interface ScheduledOperation {
  id: string;
  recipientCustomerId: string;
  amount: Decimal;
  when: Date;
  repeat?: string;
  status: string;
}
