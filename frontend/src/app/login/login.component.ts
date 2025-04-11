import {Component, OnInit, ViewChild} from '@angular/core';
import {AuthorizationService} from '../utilities/authorization.service';
import {NgIf} from "@angular/common";
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {SpinnerComponent} from "../utilities/spinner.component";
import {Router} from '@angular/router';
import {APP_CONSTANTS} from '../app.constants';

@Component({
  selector: 'app-login',
  imports: [
    NgIf,
    ReactiveFormsModule,
    SpinnerComponent
  ],
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit {
  @ViewChild(SpinnerComponent) spinner!: SpinnerComponent;

  constructor(private authorizationService: AuthorizationService, private router: Router) {
  }

  loginForm = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]), //TODO  validazione insufficiente: usare la regex del BE
    password: new FormControl('', Validators.required),
  });

  ngOnInit(): void {
    this.authorizationService.redirectIfAlreadyLoggedIn(
      () => this.spinner.hide()
    )
  }

  //TODO spinner ovunque
  onSubmit(): void {
    if (this.loginForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    //TODO popup errore login
    this.authorizationService.login(this.loginForm.controls.email.value!, this.loginForm.controls.password.value!).subscribe(
      () => {
        this.router.navigate([APP_CONSTANTS.PATH_DASHBOARD]);
      }
    );
  }

}
