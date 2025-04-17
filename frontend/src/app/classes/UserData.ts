import {PaymentCard} from './PaymentCard';

export interface UserData {
  id: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  email: string;
  lastEdit: number;
  paymentCards: PaymentCard[];
}
