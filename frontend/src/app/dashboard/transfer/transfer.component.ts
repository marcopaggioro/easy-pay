import {Component, OnInit, ViewChild} from '@angular/core';
import {NgIf} from '@angular/common';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {APP_CONSTANTS} from '../../app.constants';
import {AlertComponent} from '../../utilities/alert.component';
import {emailValidator} from '../../utilities/validators/email.validator';
import {ActivatedRoute} from '@angular/router';
import {maxTwoDecimalsValidator} from '../../utilities/validators/max-two-decimals.validator';
import {notEqualsToValidator} from '../../utilities/validators/not-equals.to.validator';
import {UserDataService} from '../../utilities/user-data.service';

@Component({
  selector: 'app-transfer',
  imports: [
    NgIf,
    ReactiveFormsModule,
    AlertComponent
  ],
  templateUrl: './transfer.component.html'
})
export class TransferComponent implements OnInit {
  @ViewChild(AlertComponent) alert!: AlertComponent;
  customerEmail!: string;
  loading = false;

  transferForm = new FormGroup({
    recipientEmail: new FormControl('', [Validators.required, Validators.email, emailValidator(), notEqualsToValidator(() => this.customerEmail)]),
    description: new FormControl('', Validators.required),
    amount: new FormControl('', [Validators.required, APP_CONSTANTS.VALIDATOR_MIN_AMOUNT, APP_CONSTANTS.VALIDATOR_MAX_AMOUNT, maxTwoDecimalsValidator()])
  });

  constructor(private http: HttpClient, private route: ActivatedRoute, private userDataService: UserDataService) {
  }

  ngOnInit() {
    const emailFromQuery = this.route.snapshot.queryParams['email'];
    this.transferForm.patchValue({recipientEmail: emailFromQuery})

    this.userDataService.userData$.subscribe(userData => {
      if (userData) {
        this.customerEmail = userData.email;
      }
    });
  }

  onSubmit(): void {
    this.loading = true;
    if (this.transferForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    const body = {
      recipientEmail: this.transferForm.value.recipientEmail!,
      description: this.transferForm.value.description!,
      amount: this.transferForm.value.amount!
    }
    this.http.post(APP_CONSTANTS.ENDPOINT_WALLET_TRANSFER, body, {
      withCredentials: true,
      responseType: 'json'
    }).subscribe({
      next: () => {
        this.transferForm.reset();

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
