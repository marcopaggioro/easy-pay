import {Component, OnInit, ViewChild} from '@angular/core';
import {SpinnerComponent} from '../../utilities/spinner.component';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {ScheduledOperation} from '../../classes/ScheduledOperation';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {DatePipe, DecimalPipe, NgClass, NgIf} from '@angular/common';
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
import {emailValidator} from '../../utilities/email.validator';
import {WebSocketService} from '../../utilities/web-socket.service';

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
    NgbTooltip,
    NgClass
  ],
  templateUrl: './scheduled-operations.component.html'
})
export class ScheduledOperationsComponent implements OnInit {
  @ViewChild(SpinnerComponent) spinner!: SpinnerComponent;
  @ViewChild(AlertComponent) alert!: AlertComponent;
  loading: boolean = false;

  scheduledOperations: ScheduledOperation[] = [];

  scheduledOperationForm = new FormGroup({
    recipientEmail: new FormControl('', [Validators.required, Validators.email, emailValidator()]),
    description: new FormControl('', Validators.required),
    amount: new FormControl('', [Validators.required, Validators.min(0.01)]),
    dateTime: new FormControl('', Validators.required)
  });


  constructor(private http: HttpClient, private webSocketService: WebSocketService) {
  }

  ngOnInit(): void {
    this.getScheduledOperations();

    this.webSocketService.getWebSocketMessages().subscribe(
      (message) => {
        if (message.event == APP_CONSTANTS.WS_SCHEDULED_OPERATIONS_UPDATED) {
          this.getScheduledOperations();
        }
      }
    );
  }

  getScheduledOperations(): void {
    this.http.get<ScheduledOperation[]>(APP_CONSTANTS.ENDPOINT_WALLET_GET_SCHEDULE, {
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
    this.http.put(APP_CONSTANTS.ENDPOINT_WALLET_CREATE_SCHEDULE, body, {
      withCredentials: true,
      responseType: 'json'
    }).subscribe({
      next: () => {
        this.scheduledOperationForm.reset();

        this.alert.success(APP_CONSTANTS.MESSAGE_SUCCESSFUL);
        this.loading = false;
      },
      error: (httpErrorResponse: HttpErrorResponse) => {
        this.alert.error(httpErrorResponse?.error?.error || APP_CONSTANTS.MESSAGE_GENERIC_ERROR);
        this.loading = false;
      }
    });
  }

  deleteScheduledOperation(id: string): void {
    this.http.delete(APP_CONSTANTS.ENDPOINT_WALLET_DELETE_SCHEDULE + "/" + id, {
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
