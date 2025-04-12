import {Component, OnInit, ViewChild} from '@angular/core';
import {SpinnerComponent} from "../../utilities/spinner.component";
import {HttpClient} from '@angular/common/http';
import {APP_CONSTANTS} from '../../app.constants';
import {DatePipe, DecimalPipe, NgIf} from '@angular/common';
import {
  NgbAccordionBody,
  NgbAccordionButton,
  NgbAccordionCollapse,
  NgbAccordionDirective,
  NgbAccordionHeader,
  NgbAccordionItem,
  NgbTooltip
} from '@ng-bootstrap/ng-bootstrap';
import {UserdataService} from '../../utilities/userdata.service';
import {RouterLink} from '@angular/router';
import {Wallet} from '../../classes/Wallet';

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
    RouterLink
  ],
  templateUrl: './wallet.component.html'
})
export class WalletComponent implements OnInit {
  @ViewChild(SpinnerComponent) spinner!: SpinnerComponent;
  customerId!: string;
  wallet?: Wallet;

  constructor(protected userDataService: UserdataService, private http: HttpClient) {
  }

  ngOnInit(): void {
    this.getWallet();

    this.userDataService.userData$.subscribe(userData => {
      if (userData) {
        this.customerId = userData.id;
      }
    });
  }

  getWallet(): void {
    this.http.get<Wallet>(APP_CONSTANTS.WALLET_GET_ENDPOINT, {withCredentials: true}).subscribe(wallet => {
      this.wallet = wallet;
      this.wallet.history = this.wallet.history.sort((a, b) =>
        new Date(b.instant).getTime() - new Date(a.instant).getTime()
      );

      this.spinner.hide();
    });
  }

  protected readonly Number = Number;
  protected readonly APP_CONSTANTS = APP_CONSTANTS;
}
