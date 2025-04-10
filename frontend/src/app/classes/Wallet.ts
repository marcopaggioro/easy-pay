import Decimal from 'decimal.js';
import {Operation} from './Operation';

export interface Wallet {
  balance: Decimal;
  history: Operation[];
}
