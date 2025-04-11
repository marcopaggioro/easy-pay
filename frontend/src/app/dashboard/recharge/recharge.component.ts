import { Component } from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {HttpClient} from '@angular/common/http';
import {NgIf} from '@angular/common';
import {APP_CONSTANTS} from '../../app.constants';

@Component({
  selector: 'app-recharge',
  imports: [
    NgIf,
    ReactiveFormsModule
  ],
  templateUrl: './recharge.component.html'
})
export class RechargeComponent {
  rechargeForm = new FormGroup({
    amount: new FormControl(0, Validators.required)
  });

  constructor(private http: HttpClient) {
  }

  onSubmit(): void {
    if (this.rechargeForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }


    this.http.post(APP_CONSTANTS.WALLET_RECHARGE_ENDPOINT, this.rechargeForm.controls.amount.value!, {
      withCredentials: true,
      responseType: 'text'
    }).subscribe(
      isLogged => {
        console.log("TODO");
      }
    );
  }
}
