import {Component, OnInit, ViewChild} from '@angular/core';
import {NgIf} from '@angular/common';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {SpinnerComponent} from '../../utilities/spinner.component';
import {AuthorizationService} from '../../utilities/authorization.service';
import {HttpClient} from '@angular/common/http';
import {UserData} from '../../classes/UserData';

@Component({
  selector: 'app-profile',
  imports: [
    NgIf,
    ReactiveFormsModule,
    SpinnerComponent
  ],
  templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
  @ViewChild(SpinnerComponent) spinner!: SpinnerComponent;
  userData!: UserData;

  constructor(private authorizationService: AuthorizationService, private http: HttpClient) {
  }

  //TODO modifica parziale dei dati
  profileForm = new FormGroup({
    name: new FormControl(this.userData?.name, Validators.required),
    surname: new FormControl(this.userData?.surname, Validators.required),
    birthDate: new FormControl(this.userData?.birthDate, Validators.required),
    email: new FormControl(this.userData?.email, [Validators.required, Validators.email]), //TODO  validazione insufficiente
    password: new FormControl('', Validators.required),
  });

  getUserData() {
    this.http.get<UserData>("http://localhost:9000/user", {withCredentials: true, responseType: 'json'}).subscribe(
      userData => {
        this.userData = userData;
        this.profileForm.patchValue({
          name: this.userData.name,
          surname: this.userData.surname,
          birthDate: this.userData.birthDate,
          email: this.userData.email
        });
        this.spinner.hide();
      }
    );
  }

  ngOnInit(): void {
    this.getUserData();
  }

  onSubmit(): void {
    this.spinner.show();

    if (this.profileForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    const body = {
      name: this.profileForm.controls.name.value,
      surname: this.profileForm.controls.surname.value,
      birthDate: this.profileForm.controls.birthDate.value,
      email: this.profileForm.controls.email.value,
      encryptedPassword: this.authorizationService.hashPassword(this.profileForm.controls.password.value!)
    }
    //TODO gestione errori e toast successo
    this.http.patch("http://localhost:9000/user", body, {withCredentials: true, responseType: 'text'}).subscribe(
      () => {
        this.profileForm.controls.password.reset();
        this.getUserData();
        this.spinner.hide();
      }
    );
  }
}
