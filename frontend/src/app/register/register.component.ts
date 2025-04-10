import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {AuthorizationService} from '../utilities/authorization.service';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {SpinnerComponent} from '../utilities/spinner.component';
import {NgIf} from '@angular/common';

@Component({
  selector: 'app-register',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    SpinnerComponent,
    NgIf
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent implements OnInit {
  @ViewChild('loading') loading!: ElementRef;

  constructor(private authorizationService: AuthorizationService) {
  }

  registerForm = new FormGroup({
    name: new FormControl('', Validators.required),
    surname: new FormControl('', Validators.required),
    email: new FormControl('', [Validators.required, Validators.email]), //TODO  validazione insufficiente
    birthDate: new FormControl('', Validators.required),
    password: new FormControl('', Validators.required),
  });

  ngOnInit(): void {
    this.authorizationService.redirectIfAlreadyLoggedIn(
      () => this.loading.nativeElement.classList.add('invisible')
    );
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    this.authorizationService.register(this.registerForm.controls.name.value || "",
      this.registerForm.controls.surname.value || "",
      this.registerForm.controls.birthDate.value || "",
      this.registerForm.controls.email.value || "",
      this.registerForm.controls.password.value || "").subscribe(
      isLogged => {
        console.log("TODO");
      }
    );
  }

}
