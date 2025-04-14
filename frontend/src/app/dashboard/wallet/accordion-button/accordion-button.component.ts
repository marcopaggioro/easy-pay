import {Component, Input} from '@angular/core';
import {Operation} from '../../../classes/Operation';
import {DatePipe, DecimalPipe, NgClass} from '@angular/common';
import {NgbAccordionButton} from '@ng-bootstrap/ng-bootstrap';
import {APP_CONSTANTS} from '../../../app.constants';

@Component({
  selector: 'app-accordion-button',
  imports: [
    DatePipe,
    DecimalPipe,
    NgbAccordionButton,
    NgClass
  ],
  templateUrl: './accordion-button.component.html'
})
export class AccordionButtonComponent {
  @Input() customerId!: string;
  @Input() operation!: Operation;

  operationClass(): string {
    if (this.operation.senderCustomerId === this.operation.recipientCustomerId) {
      return 'bg-info';
    } else if (this.customerId === this.operation.senderCustomerId) {
      return 'bg-danger';
    } else {
      return 'bg-success';
    }
  }

  protected readonly APP_CONSTANTS = APP_CONSTANTS;
  protected readonly Number = Number;
}
