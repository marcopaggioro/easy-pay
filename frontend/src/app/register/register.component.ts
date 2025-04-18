import {Component, OnInit, ViewChild} from '@angular/core';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {NgIf} from '@angular/common';
import {emailValidator} from '../utilities/validators/email.validator';
import {AlertComponent} from '../utilities/alert.component';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {APP_CONSTANTS} from '../app.constants';
import {Router, RouterLink} from '@angular/router';
import {noNumbersValidator} from '../utilities/validators/no-numbers-validator';
import {AuthorizationUtils} from '../utilities/authorization-utils';

@Component({
  selector: 'app-register',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    NgIf,
    AlertComponent,
    RouterLink
  ],
  templateUrl: './register.component.html'
})
export class RegisterComponent implements OnInit {
  @ViewChild(AlertComponent) alert!: AlertComponent;
  loading = false;

  constructor(private router: Router, private http: HttpClient) {
  }

  registerForm = new FormGroup({
    firstName: new FormControl('', [Validators.required, noNumbersValidator()]),
    lastName: new FormControl('', [Validators.required, noNumbersValidator()]),
    birthDate: new FormControl('', Validators.required),
    email: new FormControl('', [Validators.required, Validators.email, emailValidator()]),
    password: new FormControl('', [Validators.required, Validators.minLength(APP_CONSTANTS.PASSWORD_MIN_LENGHT)]),
  });

  ngOnInit(): void {
    AuthorizationUtils.redirectIfLoggedIn(this.router, this.http);
  }

  onSubmit(): void {
    this.loading = true;
    if (this.registerForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    AuthorizationUtils.register(this.http,
      this.registerForm.value.firstName!,
      this.registerForm.value.lastName!,
      this.registerForm.value.birthDate!,
      this.registerForm.value.email!,
      this.registerForm.value.password!).subscribe({
      next: () => this.router.navigate([APP_CONSTANTS.PATH_DASHBOARD]),
      error: (httpErrorResponse: HttpErrorResponse) => {
        this.alert.error(httpErrorResponse?.error?.error || APP_CONSTANTS.MESSAGE_GENERIC_ERROR);
        this.loading = false;
      }
    });
  }

  protected readonly APP_CONSTANTS = APP_CONSTANTS;
}
