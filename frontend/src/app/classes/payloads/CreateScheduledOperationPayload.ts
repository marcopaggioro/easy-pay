export interface CreateScheduledOperationPayload {
  recipientEmail: string;
  description: string;
  amount: number;
  when: string;
  repeat?: string;
}
