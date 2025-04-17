import {Component, OnInit, ViewChild} from '@angular/core';
import {NgIf} from "@angular/common";
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {Router, RouterLink} from '@angular/router';
import {APP_CONSTANTS} from '../app.constants';
import {emailValidator} from '../utilities/validators/email.validator';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {AlertComponent} from '../utilities/alert.component';
import {AuthorizationUtils} from '../utilities/authorization-utils';
import {UserDataService} from '../utilities/user-data.service';
import {CookieService} from 'ngx-cookie-service';
import {WebSocketService} from '../utilities/web-socket.service';

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

  constructor(private router: Router,
              private http: HttpClient,
              private userDataService: UserDataService,
              private cookieService: CookieService,
              private webSocketService: WebSocketService) {
  }

  loginForm = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email, emailValidator()]),
    password: new FormControl('', [Validators.required, Validators.minLength(APP_CONSTANTS.PASSWORD_MIN_LENGHT)]),
  });

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
      this.userDataService,
      this.webSocketService,
      this.cookieService,
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
}
