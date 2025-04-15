import {Component} from '@angular/core';
import {NgbAlert} from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-alert',
  imports: [
    NgbAlert
  ],
  template: `@if (alertMessage) {
    <ngb-alert [type]="alertType" (closed)="alertMessage = undefined" class="my-2">{{ alertMessage }}</ngb-alert>
  }`,
})
export class AlertComponent {
  protected alertMessage?: string;
  protected alertType = "success";

  success(message: string): void {
    this.alertType = "success";
    this.alertMessage = message;
  }

  error(message: string): void {
    this.alertType = "danger";
    this.alertMessage = message;
  }

}
