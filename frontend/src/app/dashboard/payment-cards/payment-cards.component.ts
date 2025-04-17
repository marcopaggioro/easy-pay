import {Component, OnInit, ViewChild} from '@angular/core';
import {AlertComponent} from '../../utilities/alert.component';
import {DatePipe, NgIf} from '@angular/common';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {SpinnerComponent} from '../../utilities/spinner.component';
import {UserDataService} from '../../utilities/user-data.service';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {PaymentCard} from '../../classes/PaymentCard';
import {APP_CONSTANTS} from '../../app.constants';
import {cardNumberValidator} from '../../utilities/validators/card-number.validator';
import {noNumbersValidator} from '../../utilities/validators/no-numbers-validator';

@Component({
  selector: 'app-payment-cards',
  imports: [
    AlertComponent,
    NgIf,
    ReactiveFormsModule,
    SpinnerComponent,
    DatePipe
  ],
  templateUrl: './payment-cards.component.html'
})
export class PaymentCardsComponent implements OnInit {
  @ViewChild(AlertComponent) alert!: AlertComponent;
  loading = false;
  paymentCards?: PaymentCard[];
  deletingPaymentCards: number[] = [];

  paymentCardForm = new FormGroup({
    fullName: new FormControl('', [Validators.required, noNumbersValidator()]),
    cardNumber: new FormControl('', [Validators.required, cardNumberValidator()]),
    securityCode: new FormControl('', [Validators.required, Validators.min(100), Validators.max(999)]),
    expiration: new FormControl('', Validators.required),
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

  createPaymentCard(): void {
    this.loading = true;
    if (this.paymentCardForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    const body = {
      fullName: this.paymentCardForm.value.fullName!,
      cardNumber: this.paymentCardForm.value.cardNumber!,
      securityCode: this.paymentCardForm.value.securityCode!,
      expiration: this.paymentCardForm.value.expiration!
    }
    this.http.post(APP_CONSTANTS.ENDPOINT_USER_CREATE_PAYMENT_CARD, body, {
      withCredentials: true,
      responseType: 'json'
    }).subscribe({
      next: () => {
        this.paymentCardForm.reset();
        this.alert.success(APP_CONSTANTS.MESSAGE_SUCCESSFUL);
        this.loading = false;
      },
      error: (httpErrorResponse: HttpErrorResponse) => {
        this.alert.error(httpErrorResponse?.error?.error || APP_CONSTANTS.MESSAGE_GENERIC_ERROR);
        this.loading = false;
      }
    });
  }

  deletePaymentCard(cardId: number): void {
    this.deletingPaymentCards.push(cardId);
    this.http.delete(`${APP_CONSTANTS.ENDPOINT_USER_DELETE_PAYMENT_CARD}/${cardId}`, {
      withCredentials: true,
      responseType: 'json'
    }).subscribe({
      next: () => {
        this.deletingPaymentCards.filter(value => value !== cardId);
        this.alert.success(APP_CONSTANTS.MESSAGE_SUCCESSFUL)
      },
      error: (httpErrorResponse: HttpErrorResponse) => {
        this.deletingPaymentCards.filter(value => value !== cardId);
        this.alert.error(httpErrorResponse?.error?.error || APP_CONSTANTS.MESSAGE_GENERIC_ERROR)
      }
    });
  }

}
