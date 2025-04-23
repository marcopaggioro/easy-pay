import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ReactiveFormsModule, Validators} from '@angular/forms';
import {UserDataService} from '../../utilities/user-data.service';
import {UserData} from '../../classes/UserData';
import {AlertComponent} from '../../utilities/alert.component';
import {ProfileFieldComponent} from './profile-field/profile-field.component';
import {DatePipe} from '@angular/common';
import {APP_CONSTANTS} from '../../app.constants';
import {emailValidator} from '../../utilities/validators/email.validator';
import {noNumbersValidator} from '../../utilities/validators/no-numbers-validator';
import {AuthorizationUtils} from '../../utilities/authorization-utils';
import {Subscription} from 'rxjs';

@Component({
  selector: 'app-profile',
  imports: [
    ReactiveFormsModule,
    AlertComponent,
    ProfileFieldComponent,
    DatePipe
  ],
  templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit, OnDestroy {
  @ViewChild(AlertComponent) private alert!: AlertComponent;
  private userDataSubscription?: Subscription;
  protected userData?: UserData;

  constructor(private userDataService: UserDataService) {
  }

  ngOnInit(): void {
    this.userDataSubscription = this.userDataService.userData$.subscribe(userData => userData && (this.userData = userData));
  }

  ngOnDestroy() {
    this.userDataSubscription?.unsubscribe();
  }

  handleEditResult(error: string | null): void {
    if (error) {
      this.alert.error(error);
    } else {
      this.alert.success(APP_CONSTANTS.MESSAGE_SUCCESSFUL);
    }
  }

  protected readonly emailValidator = emailValidator;
  protected readonly Validators = Validators;
  protected readonly APP_CONSTANTS = APP_CONSTANTS;
  protected readonly noNumbersValidator = noNumbersValidator;
  protected readonly AuthorizationUtils = AuthorizationUtils;
}
