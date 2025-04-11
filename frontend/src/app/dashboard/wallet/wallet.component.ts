import {Component, OnInit, ViewChild} from '@angular/core';
import {SpinnerComponent} from "../../utilities/spinner.component";
import Decimal from 'decimal.js';
import {Operation} from '../../classes/Operation';
import {AuthorizationService} from '../../utilities/authorization.service';
import {HttpClient} from '@angular/common/http';
import {Wallet} from '../../classes/Wallet';
import {APP_CONSTANTS} from '../../app.constants';
import {DatePipe, DecimalPipe, NgIf, NgOptimizedImage} from '@angular/common';
import {
  NgbAccordionBody,
  NgbAccordionButton, NgbAccordionCollapse,
  NgbAccordionDirective,
  NgbAccordionHeader,
  NgbAccordionItem, NgbTooltip
} from '@ng-bootstrap/ng-bootstrap';
import {UserdataService} from '../../utilities/userdata.service';

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
    NgOptimizedImage
  ],
  templateUrl: './wallet.component.html'
})
export class WalletComponent implements OnInit {
  @ViewChild(SpinnerComponent) spinner!: SpinnerComponent;
  balance!: Decimal;
  operationsHistory: Operation[] = [];

  constructor(protected userDataService: UserdataService, private http: HttpClient) {
  }

  ngOnInit(): void {
    this.getWallet();
  }

  getWallet(): void {
    this.http.get<Wallet>(APP_CONSTANTS.WALLET_GET_ENDPOINT, {withCredentials: true}).subscribe(wallet => {
      this.balance = wallet.balance;
      this.operationsHistory = wallet.history.sort((a, b) =>
        new Date(b.instant).getTime() - new Date(a.instant).getTime()
      );
      this.spinner.hide();
    });
  }

  protected readonly Number = Number;
  protected readonly APP_CONSTANTS = APP_CONSTANTS;
}
