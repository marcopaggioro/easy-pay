import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import {NgIf} from '@angular/common';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {APP_CONSTANTS} from '../../../app.constants';
import {FormControl, ReactiveFormsModule, ValidatorFn, Validators} from '@angular/forms';

@Component({
  selector: 'tr[app-profile-field]',
  imports: [
    NgIf,
    ReactiveFormsModule
  ],
  templateUrl: './profile-field.component.html'
})
export class ProfileFieldComponent implements OnChanges {
  @ViewChild('inputField') inputField!: ElementRef;

  @Input() fieldLabel!: string;
  @Input() httpFieldName!: string;
  @Input() fieldValue!: any;
  @Input() fieldPlaceholder!: string;
  @Input() inputType!: string;
  @Input() additionalValidators: ValidatorFn[] = [];

  @Output() editResult = new EventEmitter<string | null>();

  protected editing: boolean = false;
  protected waitingResponse: boolean = false;

  protected formField = new FormControl(null, [Validators.required, ...this.additionalValidators]);

  constructor(private http: HttpClient) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['fieldValue'] && changes['fieldValue'].currentValue !== undefined) {
      this.formField.setValue(changes['fieldValue'].currentValue);
    }
  }

  enableEditField(): void {
    this.editing = true;
    setTimeout(() => {
      this.inputField.nativeElement.focus();
    });
  }

  editField(): void {
    if (!this.waitingResponse) {
      this.waitingResponse = true;

      const body = {[this.httpFieldName]: this.formField.value!}
      this.http.patch(APP_CONSTANTS.ENDPOINT_USER_UPDATE, body, {
        withCredentials: true,
        responseType: 'json'
      }).subscribe({
        next: () => {
          this.editResult.emit(null);
          this.editing = false;
          this.waitingResponse = false;
        },
        error: (httpErrorResponse: HttpErrorResponse) => {
          this.editResult.emit(httpErrorResponse?.error?.error || APP_CONSTANTS.MESSAGE_GENERIC_ERROR);
          this.editing = false;
          this.waitingResponse = false;
        }
      });
    }
  }

}
