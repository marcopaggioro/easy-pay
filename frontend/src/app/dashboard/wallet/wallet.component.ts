import {Component, OnInit} from '@angular/core';
import {SpinnerComponent} from "../../utilities/spinner.component";
import {HttpClient} from '@angular/common/http';
import {APP_CONSTANTS} from '../../app.constants';
import {DecimalPipe, NgClass} from '@angular/common';
import {Router, RouterLink} from '@angular/router';
import {WebSocketService} from '../../utilities/web-socket.service';
import {InteractedCustomer} from '../../classes/InteractedCustomer';
import {OperationsComponent} from '../operations/operations.component';
import Decimal from 'decimal.js';

@Component({
  selector: 'app-wallet',
  imports: [
    SpinnerComponent,
    DecimalPipe,
    RouterLink,
    OperationsComponent,
    NgClass
  ],
  templateUrl: './wallet.component.html'
})
export class WalletComponent implements OnInit {
  interactedCustomers!: InteractedCustomer[];
  balance?: Decimal;

  constructor(private http: HttpClient, private router: Router, private webSocketService: WebSocketService) {
  }

  ngOnInit(): void {
    this.getBalance();
    this.getInteractedUsers();

    this.webSocketService.getWebSocketMessages().subscribe(
      (message) => {
        if (message?.type == APP_CONSTANTS.WS_WALLET_UPDATED) {
          this.getBalance();
          this.getInteractedUsers();
        }
      }
    );
  }

  getInteractedUsers(): void {
    this.http.get<InteractedCustomer[]>(APP_CONSTANTS.ENDPOINT_WALLET_GET_INTERACTED_CUSTOMERS, {
      responseType: 'json',
      withCredentials: true
    }).subscribe(interactedCustomers => this.interactedCustomers = interactedCustomers);
  }

  getBalance(): void {
    this.http.get<Decimal>(APP_CONSTANTS.ENDPOINT_WALLET_BALANCE, {withCredentials: true})
      .subscribe(balance => {
        console.log(balance)
        this.balance = balance
      });
  }

  goToTransfer(email: string): void {
    this.router.navigate([APP_CONSTANTS.PATH_TRANSFER], {queryParams: {email}});
  }

  protected readonly Number = Number;
  protected readonly APP_CONSTANTS = APP_CONSTANTS;
}
