import {Component, Input, OnChanges} from '@angular/core';
import {WalletOperation} from '../../../classes/WalletOperation';
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
export class AccordionButtonComponent implements OnChanges {
  @Input() public customerId!: string;
  @Input() public operation!: WalletOperation;
  protected otherCustomerId!: string;
  protected operationClass!: string;

  ngOnChanges(): void {
    if (this.operation.senderCustomerId === this.operation.recipientCustomerId) {
      this.otherCustomerId = this.operation.senderCustomerId;
      this.operationClass = 'text-success';
    } else if (this.customerId === this.operation.senderCustomerId) {
      this.otherCustomerId = this.operation.recipientCustomerId;
      this.operationClass = 'text-danger';
    } else {
      this.otherCustomerId = this.operation.senderCustomerId;
      this.operationClass = 'text-success';
    }
  }

  protected readonly APP_CONSTANTS = APP_CONSTANTS;
  protected readonly Number = Number;
}
