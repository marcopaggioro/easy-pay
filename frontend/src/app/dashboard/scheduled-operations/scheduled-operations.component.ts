import {Component, OnInit, ViewChild} from '@angular/core';
import {SpinnerComponent} from '../../utilities/spinner.component';
import {HttpClient} from '@angular/common/http';
import {ScheduledOperation} from '../../classes/ScheduledOperation';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {DatePipe, DecimalPipe, NgIf} from '@angular/common';
import {APP_CONSTANTS} from '../../app.constants';

@Component({
  selector: 'app-scheduled-operations',
  imports: [
    SpinnerComponent,
    FormsModule,
    NgIf,
    ReactiveFormsModule,
    DatePipe,
    DecimalPipe
  ],
  templateUrl: './scheduled-operations.component.html'
})
export class ScheduledOperationsComponent implements OnInit {
  @ViewChild(SpinnerComponent) spinner!: SpinnerComponent;
  scheduledOperations: ScheduledOperation[] = [];

  scheduledOperationForm = new FormGroup({
    recipientEmail: new FormControl('', [Validators.required, Validators.email]), //TODO  validazione insufficiente: usare la regex del BE
    description: new FormControl('', Validators.required),
    amount: new FormControl(0, Validators.required),
    dateTime: new FormControl('', Validators.required),
  });


  constructor(private http: HttpClient) {
  }

  ngOnInit(): void {
    this.getScheduledOperations();
  }

  getScheduledOperations(): void {
    this.http.get<ScheduledOperation[]>(APP_CONSTANTS.WALLET_GET_SCHEDULE_ENDPOINT, {
      withCredentials: true,
      responseType: 'json'
    }).subscribe(scheduledOperations => {
      this.scheduledOperations = scheduledOperations;
      this.spinner.hide();
    });
  }

  createScheduledOperation(): void {
    this.spinner.show();

    if (this.scheduledOperationForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    //TODO popup errore login
    const body = {
      recipientEmail: this.scheduledOperationForm.controls.recipientEmail.value!,
      description: this.scheduledOperationForm.controls.description.value!,
      amount: this.scheduledOperationForm.controls.amount.value!,
      when: this.scheduledOperationForm.controls.dateTime.value!
    }
    this.http.put(APP_CONSTANTS.WALLET_CREATE_SCHEDULE_ENDPOINT, body, {
      withCredentials: true,
      responseType: 'text'
    }).subscribe(
      () => {
        this.getScheduledOperations();
      }
    );
  }

  deleteScheduledOperation(id: string): void {
    this.spinner.show();

    this.http.delete(APP_CONSTANTS.WALLET_DELETE_SCHEDULE_ENDPOINT + "/" + id, {
      withCredentials: true,
      responseType: 'text'
    }).subscribe(
      () => {
        this.getScheduledOperations();
      }
    );
  }

  protected readonly Number = Number;
}
