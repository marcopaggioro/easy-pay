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
import {emailValidator} from '../../utilities/validators/email.validator';
import {WebSocketService} from '../../utilities/web-socket.service';
import {maxTwoDecimalsValidator} from '../../utilities/validators/max-two-decimals.validator';
import {CreateScheduledOperationPayload} from '../../classes/payloads/CreateScheduledOperationPayload';
import {notEqualsToValidator} from '../../utilities/validators/not-equals.to.validator';
import {UserDataService} from '../../utilities/user-data.service';
import {ValidationUtils} from '../../utilities/validators/validation-utils';

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
  templateUrl: './scheduler.component.html'
})
export class SchedulerComponent implements OnInit {
  @ViewChild(AlertComponent) alert!: AlertComponent;
  customerEmail!: string;
  loading = false;
  repeatToggle = false;
  scheduledOperations!: ScheduledOperation[];
  deletingOperations: string[] = [];

  scheduledOperationForm = new FormGroup({
    recipientEmail: new FormControl('', [Validators.required, emailValidator(), notEqualsToValidator(() => this.customerEmail)]),
    description: new FormControl('', Validators.required),
    amount: new FormControl<number | null>(null, [Validators.required, ValidationUtils.getMinAmountValidator(), ValidationUtils.getMaxAmountValidator(), maxTwoDecimalsValidator()]),
    dateTime: new FormControl('', Validators.required),
    repeatMonths: new FormControl('', Validators.min(1)),
    repeatDays: new FormControl('', Validators.min(1))
  });


  constructor(private http: HttpClient, private webSocketService: WebSocketService, private userDataService: UserDataService) {
  }

  ngOnInit(): void {
    this.getScheduledOperations();

    this.userDataService.userData$.subscribe(userData => userData && (this.customerEmail = userData.email));

    this.webSocketService.getWebSocketMessages().subscribe(
      (message) => {
        if (message?.type == APP_CONSTANTS.WS_SCHEDULED_OPERATIONS_UPDATED) {
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
    });
  }

  createScheduledOperation(): void {
    this.loading = true;
    if (this.scheduledOperationForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    const body: CreateScheduledOperationPayload = {
      recipientEmail: this.scheduledOperationForm.value.recipientEmail!,
      description: this.scheduledOperationForm.value.description!,
      amount: this.scheduledOperationForm.value.amount!,
      when: new Date(this.scheduledOperationForm.value.dateTime!).toISOString()
    }
    if (this.repeatToggle) {
      const months = this.scheduledOperationForm.value.repeatMonths || 0;
      const days = this.scheduledOperationForm.value.repeatDays || 0;
      body.repeat = `P${months}M${days}D`;
    }

    this.http.post(APP_CONSTANTS.ENDPOINT_WALLET_CREATE_SCHEDULE, body, {
      withCredentials: true,
      responseType: 'json'
    }).subscribe({
      next: () => {
        this.scheduledOperationForm.reset();

        this.alert.success(APP_CONSTANTS.MESSAGE_SUCCESSFUL);
        this.repeatToggle = false;
        this.loading = false;
      },
      error: (httpErrorResponse: HttpErrorResponse) => {
        this.alert.error(httpErrorResponse?.error?.error || APP_CONSTANTS.MESSAGE_GENERIC_ERROR);
        this.repeatToggle = false;
        this.loading = false;
      }
    });
  }

  deleteScheduledOperation(scheduledOperationId: string): void {
    this.deletingOperations.push(scheduledOperationId);
    this.http.delete(`${APP_CONSTANTS.ENDPOINT_WALLET_DELETE_SCHEDULE}/${scheduledOperationId}`, {
      withCredentials: true,
      responseType: 'json'
    }).subscribe({
      next: () => {
        this.deletingOperations.filter(value => value !== scheduledOperationId);
        this.alert.success(APP_CONSTANTS.MESSAGE_SUCCESSFUL)
      },
      error: (httpErrorResponse: HttpErrorResponse) => {
        this.deletingOperations.filter(value => value !== scheduledOperationId);
        this.alert.error(httpErrorResponse?.error?.error || APP_CONSTANTS.MESSAGE_GENERIC_ERROR)
      }
    });
  }

  protected readonly Number = Number;
  protected readonly APP_CONSTANTS = APP_CONSTANTS;
  protected readonly ValidationUtils = ValidationUtils;
}
