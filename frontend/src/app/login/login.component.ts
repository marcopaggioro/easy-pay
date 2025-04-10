import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {AuthorizationService} from '../utilities/authorization.service';
import {NgIf} from "@angular/common";
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {SpinnerComponent} from "../utilities/spinner.component";

@Component({
  selector: 'app-login',
  imports: [
    NgIf,
    ReactiveFormsModule,
    SpinnerComponent
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
  @ViewChild('loading') loading!: ElementRef;

  constructor(private authorizationService: AuthorizationService) {
  }

  loginForm = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]), //TODO  validazione insufficiente: usare la regex del BE
    password: new FormControl('', Validators.required),
  });

  ngOnInit(): void {
    this.authorizationService.redirectIfAlreadyLoggedIn(
      () => this.loading.nativeElement.classList.add('invisible')
    )
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      console.warn("Trying to submit with invalid data")
      return;
    }

    this.authorizationService.login(this.loginForm.controls.email.value || "",
      this.loginForm.controls.password.value || "").subscribe(
      isLogged => {
        console.log("TODO");
      }
    );
  }

}
