import {Component, OnInit, ViewChild} from '@angular/core';
import {AuthorizationService} from '../utilities/authorization.service';
import {NgIf} from "@angular/common";
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {Router, RouterLink} from '@angular/router';
import {APP_CONSTANTS} from '../app.constants';
import {emailValidator} from '../utilities/email.validator';
import {HttpErrorResponse} from '@angular/common/http';
import {AlertComponent} from '../utilities/alert.component';

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
  @ViewChild(AlertComponent) alert!: AlertComponent;
  loading = false;

  constructor(private authorizationService: AuthorizationService, private router: Router) {
  }

  loginForm = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email, emailValidator()]),
    password: new FormControl('', [Validators.required, Validators.minLength(APP_CONSTANTS.PASSWORD_MIN_LENGHT)]),
  });

  ngOnInit(): void {
    this.authorizationService.redirectIfLoggedIn();
  }

  onSubmit(): void {
    this.loading = true;
    if (this.loginForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    this.authorizationService.login(this.loginForm.value.email!, this.loginForm.value.password!).subscribe({
      next: () => this.router.navigate([APP_CONSTANTS.PATH_DASHBOARD]),
      error: (httpErrorResponse: HttpErrorResponse) => {
        this.alert.error(httpErrorResponse?.error?.error || APP_CONSTANTS.MESSAGE_GENERIC_ERROR);
        this.loading = false;
      }
    });
  }

}
