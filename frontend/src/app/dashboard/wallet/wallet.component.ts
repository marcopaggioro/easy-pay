import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {SpinnerComponent} from "../../utilities/spinner.component";
import {HttpClient} from '@angular/common/http';
import {APP_CONSTANTS} from '../../app.constants';
import {DatePipe, DecimalPipe, KeyValuePipe, NgForOf, NgIf} from '@angular/common';
import {
  NgbAccordionBody,
  NgbAccordionButton,
  NgbAccordionCollapse,
  NgbAccordionDirective,
  NgbAccordionHeader,
  NgbAccordionItem,
  NgbTooltip
} from '@ng-bootstrap/ng-bootstrap';
import {UserDataService} from '../../utilities/user-data.service';
import {Router, RouterLink} from '@angular/router';
import {Wallet} from '../../classes/Wallet';
import {WebSocketService} from '../../utilities/web-socket.service';

@Component({
  selector: 'app-wallet',
  imports: [
    SpinnerComponent,
    NgIf,
    NgbAccordionDirective,
    NgbAccordionItem,
    NgbAccordionHeader,
    NgbAccordionButton,
    NgbAccordionCollapse,
    NgbAccordionBody,
    DatePipe,
    DecimalPipe,
    NgbTooltip,
    RouterLink,
    KeyValuePipe,
    NgForOf
  ],
  templateUrl: './wallet.component.html'
})
export class WalletComponent implements OnInit {
  @ViewChild('historySpinner') historySpinner!: SpinnerComponent;
  @ViewChild('interactedUsersSpinner') interactedUsersSpinner!: SpinnerComponent;

  @ViewChild('balancePlaceholder') balancePlaceholder!: ElementRef;
  customerId!: string;
  wallet?: Wallet;
  interactedCustomers: Map<string, [string, string]> = new Map<string, [string, string]>();

  constructor(protected userDataService: UserDataService, private http: HttpClient, private router: Router, private webSocketService: WebSocketService) {
  }

  ngOnInit(): void {
    this.getWallet();

    this.userDataService.userData$.subscribe(userData => {
      if (userData) {
        this.customerId = userData.id;
      }
    });

    this.webSocketService.getWebSocketMessages().subscribe(
      (message) => {
        if (message.type == APP_CONSTANTS.WS_WALLET_UPDATED) {
          this.getWallet();
        }
      }
    );
  }

  getWallet(): void {
    this.http.get<Wallet>(APP_CONSTANTS.ENDPOINT_WALLET_GET, {withCredentials: true}).subscribe(wallet => {
      this.wallet = wallet;
      this.wallet.history = this.wallet.history.sort((a, b) =>
        new Date(b.instant).getTime() - new Date(a.instant).getTime()
      );

      this.wallet!.history
        .filter(op => op.senderCustomerId !== op.recipientCustomerId)
        .forEach(op => {
          const fullName = `${op.interactedFirstName} ${op.interactedLastName}`;
          if (op.senderCustomerId == this.customerId) {
            this.interactedCustomers.set(op.recipientCustomerId, [op.interactedEmail, fullName]);
          } else {
            this.interactedCustomers.set(op.senderCustomerId, [op.interactedEmail, fullName]);
          }
        });

      this.historySpinner.hide();
      this.interactedUsersSpinner.hide();
    });
  }

  goToTransfer(email: string): void {
    this.router.navigate([APP_CONSTANTS.PATH_TRANSFER], {queryParams: {email}});
  }

  protected readonly Number = Number;
  protected readonly APP_CONSTANTS = APP_CONSTANTS;
}
