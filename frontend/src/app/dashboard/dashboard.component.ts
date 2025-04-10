import {Component, OnInit} from '@angular/core';
import {AuthorizationService} from '../utilities/authorization.service';
import Decimal from 'decimal.js';
import {Operation} from '../classes/Operation';
import {HttpClient} from '@angular/common/http';
import {Wallet} from '../classes/Wallet';
import {SpinnerComponent} from '../utilities/spinner.component';

@Component({
  selector: 'app-dashboard',
  imports: [
    SpinnerComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  balance!: Decimal;
  operationsHistory: Operation[] = [];

  constructor(private authorizationService: AuthorizationService, private http: HttpClient ) {
  }

  ngOnInit(): void {
    this.authorizationService.redirectIfNotLoggedIn();
    this.getWallet();
  }

  getWallet(): void {
    this.http.get<Wallet>("http://localhost:9000/wallet", {withCredentials: true}).subscribe(wallet => {
      this.balance = wallet.balance;
      this.operationsHistory = wallet.history;
    });
  }

}
