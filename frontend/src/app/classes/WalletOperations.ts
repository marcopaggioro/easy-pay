import {WalletOperation} from './WalletOperation';

export interface WalletOperations {
  historyCount: number;
  history: WalletOperation[];
  pageSize: number;
}
