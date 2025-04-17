import {Component, OnInit, ViewChild} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {NgIf} from '@angular/common';
import {APP_CONSTANTS} from '../../app.constants';
import {AlertComponent} from '../../utilities/alert.component';
import {maxTwoDecimalsValidator} from '../../utilities/validators/max-two-decimals.validator';
import {UserDataService} from '../../utilities/user-data.service';
import {PaymentCard} from '../../classes/PaymentCard';
import {RouterLink} from '@angular/router';

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
export class RechargeComponent implements OnInit {
  @ViewChild(AlertComponent) alert!: AlertComponent;
  paymentCards?: PaymentCard[];
  loading = false;

  rechargeForm = new FormGroup({
    cardId: new FormControl('', Validators.required),
    amount: new FormControl('', [Validators.required, APP_CONSTANTS.VALIDATOR_MIN_AMOUNT, APP_CONSTANTS.VALIDATOR_MAX_AMOUNT, maxTwoDecimalsValidator()])
  });

  constructor(private http: HttpClient, private userDataService: UserDataService) {
  }

  ngOnInit() {
    this.userDataService.userData$.subscribe(userData => {
      if (userData) {
        this.paymentCards = userData.paymentCards;
      }
    });
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
}
