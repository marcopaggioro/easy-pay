import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {NgIf} from '@angular/common';
import {APP_CONSTANTS} from '../../app.constants';
import {AlertComponent} from '../../utilities/alert.component';
import {maxTwoDecimalsValidator} from '../../utilities/validators/max-two-decimals.validator';
import {UserDataService} from '../../utilities/user-data.service';
import {RouterLink} from '@angular/router';
import {ValidationUtils} from '../../utilities/validators/validation-utils';
import {PaymentCard} from '../../classes/PaymentCard';
import {Subscription} from 'rxjs';

@Component({
  selector: 'app-recharge',
  imports: [
    NgIf,
    ReactiveFormsModule,
    AlertComponent,
    RouterLink
  ],
  templateUrl: './recharge.component.html'
})
export class RechargeComponent implements OnInit, OnDestroy {
  @ViewChild(AlertComponent) private alert!: AlertComponent;
  private userDataSubscription?: Subscription;
  protected paymentCards: PaymentCard[] = [];
  protected loading = false;

  protected rechargeForm = new FormGroup({
    cardId: new FormControl('', Validators.required),
    amount: new FormControl('', [Validators.required, ValidationUtils.getMinAmountValidator(), ValidationUtils.getMaxAmountValidator(), maxTwoDecimalsValidator()])
  });

  constructor(private http: HttpClient, private userDataService: UserDataService) {
  }

  ngOnInit() {
    this.userDataSubscription = this.userDataService.userData$.subscribe(userData => userData && (this.paymentCards = userData.paymentCards));
  }

  ngOnDestroy() {
    this.userDataSubscription?.unsubscribe();
  }

  onSubmit(): void {
    this.loading = true;
    if (this.rechargeForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }


    const body = {
      cardId: this.rechargeForm.value.cardId!,
      amount: this.rechargeForm.value.amount!
    }
    this.http.post(APP_CONSTANTS.ENDPOINT_WALLET_RECHARGE, body, {
      withCredentials: true,
      responseType: 'json'
    }).subscribe({
      next: () => {
        this.rechargeForm.reset();

        this.alert.success(APP_CONSTANTS.MESSAGE_SUCCESSFUL);
        this.loading = false;
      },
      error: (httpErrorResponse: HttpErrorResponse) => {
        this.alert.error(httpErrorResponse?.error?.error || APP_CONSTANTS.MESSAGE_GENERIC_ERROR);
        this.loading = false;
      }
    });
  }

  protected readonly ValidationUtils = ValidationUtils;
}
