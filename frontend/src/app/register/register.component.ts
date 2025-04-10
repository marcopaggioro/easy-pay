import {Component, OnInit, ViewChild} from '@angular/core';
import {AuthorizationService} from '../utilities/authorization.service';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {SpinnerComponent} from '../utilities/spinner.component';
import {NgIf} from '@angular/common';
import {Router} from '@angular/router';

@Component({
  selector: 'app-register',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    SpinnerComponent,
    NgIf
  ],
  templateUrl: './register.component.html'
})
export class RegisterComponent implements OnInit {
  @ViewChild(SpinnerComponent) spinner!: SpinnerComponent;

  constructor(private authorizationService: AuthorizationService, private router: Router) {
  }

  registerForm = new FormGroup({
    name: new FormControl('', Validators.required),
    surname: new FormControl('', Validators.required),
    birthDate: new FormControl('', Validators.required),
    email: new FormControl('', [Validators.required, Validators.email]), //TODO  validazione insufficiente
    password: new FormControl('', Validators.required),
  });

  ngOnInit(): void {
    this.authorizationService.redirectIfAlreadyLoggedIn(
      () => this.spinner.hide()
    );
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    this.authorizationService.register(this.registerForm.controls.name.value!,
      this.registerForm.controls.surname.value!,
      this.registerForm.controls.birthDate.value!,
      this.registerForm.controls.email.value!,
      this.registerForm.controls.password.value!).subscribe(
      isLogged => {
        this.router.navigate(["./dashboard"])
      }
    );
  }

}
