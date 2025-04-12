import {Component, OnInit, ViewChild} from '@angular/core';
import {AuthorizationService} from '../utilities/authorization.service';
import {NgIf} from "@angular/common";
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {Router} from '@angular/router';
import {APP_CONSTANTS} from '../app.constants';
import {emailValidator} from '../utilities/email.validator';
import {HttpErrorResponse} from '@angular/common/http';
import {AlertComponent} from '../utilities/alert.component';

@Component({
  selector: 'app-login',
  imports: [
    NgIf,
    ReactiveFormsModule,
    AlertComponent
  ],
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit {
  @ViewChild(AlertComponent) alert!: AlertComponent;
  loading: boolean = false;

  constructor(private authorizationService: AuthorizationService, private router: Router) {
  }

  loginForm = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email, emailValidator()]),
    password: new FormControl('', Validators.required),
  });

  ngOnInit(): void {
    this.authorizationService.redirectIfAlreadyLoggedIn(() => {
    });
  }

  onSubmit(): void {
    this.loading = true;
    if (this.loginForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    this.authorizationService.login(this.loginForm.controls.email.value!, this.loginForm.controls.password.value!).subscribe({
      next: () => {
        this.router.navigate([APP_CONSTANTS.PATH_DASHBOARD]);
      },
      error: (httpErrorResponse: HttpErrorResponse) => {
        this.alert.error(httpErrorResponse?.error?.error || "Errore generico");
        this.loading = false;
      }
    });
  }

}
