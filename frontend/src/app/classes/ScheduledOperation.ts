import Decimal from 'decimal.js';

export interface ScheduledOperation {
  id: string;
  recipientCustomerId: string;
  amount: Decimal;
  when: number;
  description: string;
  interactedFirstName: string;
  interactedLastName: string;
  interactedEmail: string;
  repeat?: string;
  status: string;
}
