import {Component, Input} from '@angular/core';
import {DatePipe, DecimalPipe} from "@angular/common";
import {NgbAccordionBody, NgbTooltip} from "@ng-bootstrap/ng-bootstrap";
import {Operation} from '../../../classes/Operation';

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
  @Input() customerId!: string;
  @Input() operation!: Operation;


  protected readonly Number = Number;
}
