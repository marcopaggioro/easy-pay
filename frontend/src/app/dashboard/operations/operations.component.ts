import {Component, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {AccordionBodyComponent} from './accordion-body/accordion-body.component';
import {AccordionButtonComponent} from './accordion-button/accordion-button.component';
import {
  NgbAccordionCollapse,
  NgbAccordionDirective,
  NgbAccordionHeader,
  NgbAccordionItem,
  NgbPagination
} from '@ng-bootstrap/ng-bootstrap';
import {SpinnerComponent} from '../../utilities/spinner.component';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {APP_CONSTANTS} from '../../app.constants';
import {WalletOperations} from '../../classes/WalletOperations';
import {WebSocketService} from '../../utilities/web-socket.service';
import {NgClass, NgIf} from '@angular/common';
import {RouterLink} from '@angular/router';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {emailValidator} from '../../utilities/validators/email.validator';
import {GetWalletOperationsPayload} from '../../classes/payloads/GetWalletOperationsPayload';
import {AlertComponent} from '../../utilities/alert.component';
import {noNumbersValidator} from '../../utilities/validators/no-numbers-validator';
import {UserDataService} from '../../utilities/user-data.service';
import {ValidationUtils} from '../../utilities/validators/validation-utils';
import {Subscription} from 'rxjs';

@Component({
  selector: 'app-operations',
  imports: [
    AccordionBodyComponent,
    AccordionButtonComponent,
    NgbAccordionCollapse,
    NgbAccordionDirective,
    NgbAccordionHeader,
    NgbAccordionItem,
    NgbPagination,
    SpinnerComponent,
    NgClass,
    RouterLink,
    ReactiveFormsModule,
    NgIf,
    AlertComponent
  ],
  templateUrl: './operations.component.html'
})
export class OperationsComponent implements OnInit, OnDestroy {
  @ViewChild(AlertComponent) private alert?: AlertComponent;
  private userDataSubscription?: Subscription;
  private wsSubscription?: Subscription;

  protected customerId!: string;
  protected operations!: WalletOperations;
  protected page = 1;
  protected loading = true;

  @Input() public cardTitle = "Storico operazioni";
  @Input() public completeNavigation = true;
  @Input() public cardClasses = "col col-md-10 col-lg-8 col-xl-6 col-xxl-5";

  protected operationsForm = new FormGroup({
    fullName: new FormControl<string | null>(null, noNumbersValidator()),
    email: new FormControl<string | null>(null, emailValidator()),
    startDate: new FormControl<Date | null>(null),
    endDate: new FormControl<Date | null>(null)
  });

  constructor(private http: HttpClient,
              private webSocketService: WebSocketService,
              private userDataService: UserDataService) {
  }

  ngOnInit(): void {
    this.getOperations();

    this.userDataSubscription = this.userDataService.userData$.subscribe(userData => userData && (this.customerId = userData.id));

    this.wsSubscription = this.webSocketService.getWebSocketMessages().subscribe(
      (message) => {
        if (message?.type == APP_CONSTANTS.WS_WALLET_UPDATED) {
          this.getOperations();
        }
      }
    );
  }

  ngOnDestroy() {
    this.userDataSubscription?.unsubscribe();
    this.wsSubscription?.unsubscribe();
  }

  getOperations(): void {
    this.loading = true;
    if (this.operationsForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    const body: GetWalletOperationsPayload = {
      page: this.page,
      ...(this.operationsForm.value.email ? {email: this.operationsForm.value.email} : {}),
      ...(this.operationsForm.value.fullName ? {fullName: this.operationsForm.value.fullName} : {}),
      ...(this.operationsForm.value.startDate ? {start: new Date(this.operationsForm.value.startDate).toISOString()} : {}),
      ...(this.operationsForm.value.endDate ? {end: new Date(this.operationsForm.value.endDate).toISOString()} : {}),
    };
    this.http.post<WalletOperations>(APP_CONSTANTS.ENDPOINT_WALLET_OPERATIONS, body, {
      withCredentials: true
    }).subscribe({
      next: operations => {
        this.alert?.hide();
        this.operations = operations;
        this.loading = false;
      },
      error: (httpErrorResponse: HttpErrorResponse) => {
        this.alert?.error(httpErrorResponse?.error?.error || APP_CONSTANTS.MESSAGE_GENERIC_ERROR);
        this.loading = false;
      }
    });
  }

  protected readonly ValidationUtils = ValidationUtils;
}
