import { Component } from '@angular/core';
import {NgIf} from '@angular/common';
import {ReactiveFormsModule} from '@angular/forms';

@Component({
  selector: 'app-profile',
  imports: [
    NgIf,
    ReactiveFormsModule
  ],
  templateUrl: './profile.component.html'
})
export class ProfileComponent {

}
