import {Component, ViewChild} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {NgIf} from '@angular/common';
import {APP_CONSTANTS} from '../../app.constants';
import {AlertComponent} from '../../utilities/alert.component';
import {maxTwoDecimalsValidator} from '../../utilities/maxTwoDecimals.validator';

@Component({
  selector: 'app-recharge',
  imports: [
    NgIf,
    ReactiveFormsModule,
    AlertComponent
  ],
  templateUrl: './recharge.component.html'
})
export class RechargeComponent {
  @ViewChild(AlertComponent) alert!: AlertComponent;
  loading: boolean = false;

  rechargeForm = new FormGroup({
    amount: new FormControl('', [Validators.required, Validators.min(0.01), maxTwoDecimalsValidator()])
  });

  constructor(private http: HttpClient) {
  }

  onSubmit(): void {
    this.loading = true;
    if (this.rechargeForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }


    this.http.post(APP_CONSTANTS.ENDPOINT_WALLET_RECHARGE, this.rechargeForm.controls.amount.value!, {
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
