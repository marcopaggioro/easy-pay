import {Component, OnInit, ViewChild} from '@angular/core';
import {SpinnerComponent} from "../../utilities/spinner.component";
import Decimal from 'decimal.js';
import {Operation} from '../../classes/Operation';
import {AuthorizationService} from '../../utilities/authorization.service';
import {HttpClient} from '@angular/common/http';
import {Wallet} from '../../classes/Wallet';

@Component({
  selector: 'app-wallet',
    imports: [
        SpinnerComponent
    ],
  templateUrl: './wallet.component.html'
})
export class WalletComponent implements OnInit {
  @ViewChild(SpinnerComponent) spinner!: SpinnerComponent;
  balance!: Decimal;
  operationsHistory: Operation[] = [];

  constructor(private http: HttpClient) {
  }

  ngOnInit(): void {
    this.getWallet();
  }

  getWallet(): void {
    this.http.get<Wallet>("http://localhost:9000/wallet", {withCredentials: true}).subscribe(wallet => {
      this.balance = wallet.balance;
      this.operationsHistory = wallet.history;
      this.spinner.hide();
    });
  }
}
