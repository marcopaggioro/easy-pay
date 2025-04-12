import {Component, OnInit, ViewChild} from '@angular/core';
import {NgIf} from '@angular/common';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {SpinnerComponent} from '../../utilities/spinner.component';
import {AuthorizationService} from '../../utilities/authorization.service';
import {HttpClient} from '@angular/common/http';
import {APP_CONSTANTS} from '../../app.constants';
import {UserdataService} from '../../utilities/userdata.service';
import {emailValidator} from '../../utilities/email.validator';

@Component({
  selector: 'app-profile',
  imports: [
    NgIf,
    ReactiveFormsModule
  ],
  templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
  //TODO modifica parziale dei dati

  @ViewChild(SpinnerComponent) spinner!: SpinnerComponent;

  constructor(private authorizationService: AuthorizationService, protected userDataService: UserdataService, private http: HttpClient) {
  }

  profileForm = new FormGroup({
    firstName: new FormControl('', Validators.required),
    lastName: new FormControl('', Validators.required),
    birthDate: new FormControl<Date | null>(null, Validators.required),
    email: new FormControl('', [Validators.required, Validators.email, emailValidator()]),
    password: new FormControl('', Validators.required)
  });

  ngOnInit(): void {
    this.userDataService.userData$.subscribe(userData => {
      if (userData) {
        this.profileForm.patchValue({
          firstName: userData.firstName,
          lastName: userData.lastName,
          birthDate: userData.birthDate,
          email: userData.email
        });
      }
    });
  }

  onSubmit(): void {
    if (this.profileForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    const body = {
      firstName: this.profileForm.controls.firstName.value,
      lastName: this.profileForm.controls.lastName.value,
      birthDate: this.profileForm.controls.birthDate.value,
      email: this.profileForm.controls.email.value,
      encryptedPassword: this.authorizationService.hashPassword(this.profileForm.controls.password.value!)
    }
    //TODO gestione errori e toast successo
    this.http.patch(APP_CONSTANTS.USER_UPDATE_ENDPOINT, body, {withCredentials: true, responseType: 'text'}).subscribe(
      () => {
        this.profileForm.controls.password.reset();
        this.spinner.hide();
      }
    );
  }
}
