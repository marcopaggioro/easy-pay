import {Component} from '@angular/core';
import {NgIf} from '@angular/common';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {HttpClient} from '@angular/common/http';
import {APP_CONSTANTS} from '../../app.constants';

@Component({
  selector: 'app-transfer',
  imports: [
    NgIf,
    ReactiveFormsModule
  ],
  templateUrl: './transfer.component.html'
})
export class TransferComponent {
  transferForm = new FormGroup({
    recipientEmail: new FormControl('', [Validators.required, Validators.email]), //TODO  validazione insufficiente: usare la regex del BE
    amount: new FormControl(0, Validators.required),
  });

  constructor(private http: HttpClient) {
  }

  onSubmit(): void {
    if (this.transferForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }


    const body = {
      recipientEmail: this.transferForm.controls.recipientEmail.value || "",
      amount: this.transferForm.controls.amount.value || 0
    }
    this.http.post(APP_CONSTANTS.WALLET_TRANSFER_ENDPOINT, body, {
      withCredentials: true,
      responseType: 'text'
    }).subscribe(
      isLogged => {
        console.log("TODO");
      }
    );
  }

}
