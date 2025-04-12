import {Component, OnInit, ViewChild} from '@angular/core';
import {AuthorizationService} from '../utilities/authorization.service';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {NgIf} from '@angular/common';
import {emailValidator} from '../utilities/email.validator';
import {AlertComponent} from '../utilities/alert.component';
import {HttpErrorResponse} from '@angular/common/http';
import {APP_CONSTANTS} from '../app.constants';

@Component({
  selector: 'app-register',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    NgIf,
    AlertComponent
  ],
  templateUrl: './register.component.html'
})
export class RegisterComponent implements OnInit {
  @ViewChild(AlertComponent) alert!: AlertComponent;
  loading: boolean = false;

  constructor(private authorizationService: AuthorizationService) {
  }

  registerForm = new FormGroup({
    firstName: new FormControl('', Validators.required),
    lastName: new FormControl('', Validators.required),
    birthDate: new FormControl('', Validators.required),
    email: new FormControl('', [Validators.required, Validators.email, emailValidator()]),
    password: new FormControl('', [Validators.required, Validators.minLength(APP_CONSTANTS.PASSWORD_MIN_LENGHT)]),
  });

  ngOnInit(): void {
    this.authorizationService.redirectIfAlreadyLoggedIn();
  }

  onSubmit(): void {
    this.loading = true;
    if (this.registerForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    this.authorizationService.register(this.registerForm.controls.firstName.value!,
      this.registerForm.controls.lastName.value!,
      this.registerForm.controls.birthDate.value!,
      this.registerForm.controls.email.value!,
      this.registerForm.controls.password.value!).subscribe({
      next: () => {
        //TODO deve aspettare notifica ws per la fine della proiezione
        // this.router.navigate([APP_CONSTANTS.PATH_DASHBOARD]);
      },
      error: (httpErrorResponse: HttpErrorResponse) => {
        this.alert.error(httpErrorResponse?.error?.error || APP_CONSTANTS.MESSAGE_GENERIC_ERROR);
        this.loading = false;
      }
    });
  }

}
