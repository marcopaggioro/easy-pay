import {Component, OnInit, ViewChild} from '@angular/core';
import {NgIf} from '@angular/common';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {SpinnerComponent} from '../../utilities/spinner.component';
import {AuthorizationService} from '../../utilities/authorization.service';
import {HttpClient} from '@angular/common/http';
import {UserData} from '../../classes/UserData';
import {APP_CONSTANTS} from '../../app.constants';
import {UserdataService} from '../../utilities/userdata.service';

@Component({
  selector: 'app-profile',
  imports: [
    NgIf,
    ReactiveFormsModule,
    SpinnerComponent
  ],
  templateUrl: './profile.component.html'
})
export class ProfileComponent {
  @ViewChild(SpinnerComponent) spinner!: SpinnerComponent;

  constructor(private authorizationService: AuthorizationService, private userDataService: UserdataService, private http: HttpClient) {
  }

  //TODO modifica parziale dei dati
  /*
  *     firstName: new FormControl(this.userDataService?.userData!.firstName, Validators.required),
    lastName: new FormControl(this.userData?.lastName, Validators.required),
    birthDate: new FormControl(this.userData?.birthDate, Validators.required),
    email: new FormControl(this.userData?.email, [Validators.required, Validators.email]), //TODO  validazione insufficiente
    password: new FormControl('', Validators.required),
  * */
  profileForm = new FormGroup({
    firstName: new FormControl('', Validators.required),
    lastName: new FormControl('', Validators.required),
    birthDate: new FormControl('', Validators.required),
    email: new FormControl('', [Validators.required, Validators.email]), //TODO  validazione insufficiente
    password: new FormControl('', Validators.required),
  });

  //TODO
  // this.profileForm.patchValue({
  //                               firstName: this.userData.firstName,
  //                               lastName: this.userData.lastName,
  //                               birthDate: this.userData.birthDate,
  //                               email: this.userData.email
  //                             });

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
