import {Component, Input} from '@angular/core';
import {DatePipe, DecimalPipe} from "@angular/common";
import {NgbAccordionBody, NgbTooltip} from "@ng-bootstrap/ng-bootstrap";
import {WalletOperation} from '../../../classes/WalletOperation';

@Component({
  selector: 'app-accordion-body',
  imports: [
    DatePipe,
    DecimalPipe,
    NgbAccordionBody,
    NgbTooltip
  ],
  templateUrl: './accordion-body.component.html'
})
export class AccordionBodyComponent {
  @Input() public customerId!: string;
  @Input() public operation!: WalletOperation;


  protected readonly Number = Number;
}
