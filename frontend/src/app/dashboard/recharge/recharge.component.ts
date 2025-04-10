import { Component } from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {HttpClient} from '@angular/common/http';
import {NgIf} from '@angular/common';

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


    this.http.post("http://localhost:9000/wallet/recharge", this.rechargeForm.controls.amount.value || 0, {
      withCredentials: true,
      responseType: 'text'
    }).subscribe(
      isLogged => {
        console.log("TODO");
      }
    );
  }
}
