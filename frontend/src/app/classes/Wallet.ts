import Decimal from 'decimal.js';
import {Operation} from './Operation';

export interface Wallet {
  balance: Decimal;
  pageSize: number;
  historyCount: number;
  history: Operation[];
}
