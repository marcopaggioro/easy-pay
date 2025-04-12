import {Component, OnInit, ViewChild} from '@angular/core';
import {SpinnerComponent} from '../../utilities/spinner.component';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {ScheduledOperation} from '../../classes/ScheduledOperation';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {DatePipe, DecimalPipe, NgIf} from '@angular/common';
import {APP_CONSTANTS} from '../../app.constants';
import {AlertComponent} from '../../utilities/alert.component';
import {
  NgbAccordionBody,
  NgbAccordionButton,
  NgbAccordionCollapse,
  NgbAccordionDirective,
  NgbAccordionHeader,
  NgbAccordionItem,
  NgbTooltip
} from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-scheduled-operations',
  imports: [
    SpinnerComponent,
    FormsModule,
    NgIf,
    ReactiveFormsModule,
    DatePipe,
    DecimalPipe,
    AlertComponent,
    NgbAccordionBody,
    NgbAccordionButton,
    NgbAccordionCollapse,
    NgbAccordionDirective,
    NgbAccordionHeader,
    NgbAccordionItem,
    NgbTooltip
  ],
  templateUrl: './scheduled-operations.component.html'
})
export class ScheduledOperationsComponent implements OnInit {
  @ViewChild(SpinnerComponent) spinner!: SpinnerComponent;
  @ViewChild(AlertComponent) alert!: AlertComponent;
  loading: boolean = false;

  scheduledOperations: ScheduledOperation[] = [];

  scheduledOperationForm = new FormGroup({
    recipientEmail: new FormControl('', [Validators.required, Validators.email]), //TODO  validazione insufficiente: usare la regex del BE
    description: new FormControl('', Validators.required),
    amount: new FormControl(0, Validators.required),
    dateTime: new FormControl<Date | null>(null, Validators.required)
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
    this.loading = true;
    if (this.scheduledOperationForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    const body = {
      recipientEmail: this.scheduledOperationForm.controls.recipientEmail.value!,
      description: this.scheduledOperationForm.controls.description.value!,
      amount: this.scheduledOperationForm.controls.amount.value!,
      when: new Date(this.scheduledOperationForm.controls.dateTime.value!).toISOString()
    }
    this.http.put(APP_CONSTANTS.WALLET_CREATE_SCHEDULE_ENDPOINT, body, {
      withCredentials: true,
      responseType: 'json'
    }).subscribe({
      next: () => {
        this.scheduledOperationForm.reset();

        this.alert.success("Operazione andata a buon fine");
        this.loading = false;
      },
      error: (httpErrorResponse: HttpErrorResponse) => {
        this.alert.error(httpErrorResponse?.error?.error || "Errore generico");
        this.loading = false;
      }
    });
  }

  deleteScheduledOperation(id: string): void {
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
  protected readonly APP_CONSTANTS = APP_CONSTANTS;
}
