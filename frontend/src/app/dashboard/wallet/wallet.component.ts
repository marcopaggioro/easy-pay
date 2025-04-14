import {Component, ElementRef, OnChanges, OnInit, SimpleChanges, ViewChild} from '@angular/core';
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
  NgbAccordionItem, NgbPagination,
  NgbTooltip
} from '@ng-bootstrap/ng-bootstrap';
import {UserDataService} from '../../utilities/user-data.service';
import {Router, RouterLink} from '@angular/router';
import {Wallet} from '../../classes/Wallet';
import {WebSocketService} from '../../utilities/web-socket.service';
import {InteractedCustomer} from '../../classes/InteractedCustomer';

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
    NgForOf,
    NgbPagination
  ],
  templateUrl: './wallet.component.html'
})
export class WalletComponent implements OnInit {
  @ViewChild('historySpinner') historySpinner!: SpinnerComponent;
  @ViewChild('interactedUsersSpinner') interactedUsersSpinner!: SpinnerComponent;

  @ViewChild('balancePlaceholder') balancePlaceholder!: ElementRef;
  customerId!: string;
  wallet?: Wallet;
  interactedCustomers: InteractedCustomer[] = [];
  page: number = 1;

  constructor(protected userDataService: UserDataService, private http: HttpClient, private router: Router, private webSocketService: WebSocketService) {
  }

  ngOnInit(): void {
    this.getWallet();
    this.getInteractedUsers();

    this.userDataService.userData$.subscribe(userData => {
      if (userData) {
        this.customerId = userData.id;
      }
    });

    this.webSocketService.getWebSocketMessages().subscribe(
      (message) => {
        if (message.type == APP_CONSTANTS.WS_WALLET_UPDATED) {
          this.getWallet();
          this.getInteractedUsers();
        }
      }
    );
  }

  getInteractedUsers(): void {
    this.http.get<InteractedCustomer[]>(APP_CONSTANTS.ENDPOINT_WALLET_GET_INTERACTED_CUSTOMERS, {
      responseType: 'json',
      withCredentials: true
    }).subscribe(interactedCustomers => {
      this.interactedCustomers = interactedCustomers;
      this.interactedUsersSpinner.hide();
    });
  }

  getWallet(): void {
    this.http.get<Wallet>(APP_CONSTANTS.ENDPOINT_WALLET_GET, {
      params: {page: this.page},
      withCredentials: true
    }).subscribe(wallet => {
      this.wallet = wallet;
      this.historySpinner.hide();
    });
  }

  goToTransfer(email: string): void {
    this.router.navigate([APP_CONSTANTS.PATH_TRANSFER], {queryParams: {email}});
  }

  protected readonly Number = Number;
  protected readonly APP_CONSTANTS = APP_CONSTANTS;
}
