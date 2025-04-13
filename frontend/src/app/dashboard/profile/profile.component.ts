import {Component, OnInit, ViewChild} from '@angular/core';
import {ReactiveFormsModule, Validators} from '@angular/forms';
import {SpinnerComponent} from '../../utilities/spinner.component';
import {UserDataService} from '../../utilities/user-data.service';
import {UserData} from '../../classes/UserData';
import {AlertComponent} from '../../utilities/alert.component';
import {ProfileFieldComponent} from './profile-field/profile-field.component';
import {DatePipe} from '@angular/common';
import {APP_CONSTANTS} from '../../app.constants';
import {emailValidator} from '../../utilities/email.validator';

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
export class ProfileComponent implements OnInit {
  @ViewChild(AlertComponent) alert!: AlertComponent;
  userData?: UserData;

  constructor(protected userDataService: UserDataService) {
  }

  ngOnInit(): void {
    this.userDataService.userData$.subscribe(userData => {
      if (userData) {
        this.userData = userData;
      }
    });
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
}
