import {Component, OnInit, ViewChild} from '@angular/core';
import {NgIf} from "@angular/common";
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {Router, RouterLink} from '@angular/router';
import {APP_CONSTANTS} from '../app.constants';
import {emailValidator} from '../utilities/validators/email.validator';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {AlertComponent} from '../utilities/alert.component';
import {AuthorizationUtils} from '../utilities/authorization-utils';
import {ValidationUtils} from '../utilities/validators/validation-utils';

@Component({
  selector: 'app-login',
  imports: [
    NgIf,
    ReactiveFormsModule,
    AlertComponent,
    RouterLink
  ],
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit {
  @ViewChild(AlertComponent) private alert!: AlertComponent;
  protected loading = false;

  protected loginForm = new FormGroup({
    email: new FormControl('', [Validators.required, emailValidator()]),
    password: new FormControl('', [Validators.required, Validators.minLength(APP_CONSTANTS.PASSWORD_MIN_LENGHT)]),
  });

  constructor(private router: Router, private http: HttpClient) {
  }

  ngOnInit(): void {
    AuthorizationUtils.redirectIfLoggedIn(this.router, this.http);
  }

  onSubmit(): void {
    this.loading = true;
    if (this.loginForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    AuthorizationUtils.login(this.http,
      this.loginForm.value.email!,
      this.loginForm.value.password!)
      .subscribe({
        next: () => this.router.navigate([APP_CONSTANTS.PATH_DASHBOARD]),
        error: (httpErrorResponse: HttpErrorResponse) => {
          this.alert.error(httpErrorResponse?.error?.error || APP_CONSTANTS.MESSAGE_GENERIC_ERROR);
          this.loading = false;
        }
      });
  }

  protected readonly APP_CONSTANTS = APP_CONSTANTS;
  protected readonly ValidationUtils = ValidationUtils;
}
