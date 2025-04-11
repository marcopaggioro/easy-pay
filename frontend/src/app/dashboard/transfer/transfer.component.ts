import {Component, ViewChild} from '@angular/core';
import {NgIf} from '@angular/common';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {APP_CONSTANTS} from '../../app.constants';
import {AlertComponent} from '../../utilities/alert.component';

@Component({
  selector: 'app-transfer',
  imports: [
    NgIf,
    ReactiveFormsModule,
    AlertComponent
  ],
  templateUrl: './transfer.component.html'
})
export class TransferComponent {
  @ViewChild(AlertComponent) alert!: AlertComponent;
  loading: boolean = false;

  transferForm = new FormGroup({
    recipientEmail: new FormControl('', [Validators.required, Validators.email]), //TODO  validazione insufficiente: usare la regex del BE
    description: new FormControl('', Validators.required),
    amount: new FormControl('', Validators.required),
  });

  constructor(private http: HttpClient) {
  }

  onSubmit(): void {
    this.loading = true;
    if (this.transferForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    const body = {
      recipientEmail: this.transferForm.controls.recipientEmail.value!,
      description: this.transferForm.controls.description.value!,
      amount: this.transferForm.controls.amount.value!
    }
    this.http.post(APP_CONSTANTS.WALLET_TRANSFER_ENDPOINT, body, {
      withCredentials: true,
      responseType: 'json'
    }).subscribe({
      next: () => {
        this.transferForm.reset();

        this.alert.success("Operazione andata a buon fine");
        this.loading = false;
      },
      error: (httpErrorResponse: HttpErrorResponse) => {
        this.alert.error(httpErrorResponse?.error?.error || "Errore generico");
        this.loading = false;
      }
    });
  }

}
