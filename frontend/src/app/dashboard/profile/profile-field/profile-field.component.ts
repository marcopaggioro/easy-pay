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
import {ValidationUtils} from '../../../utilities/validators/validation-utils';

@Component({
  selector: 'tr[app-profile-field]',
  imports: [
    NgIf,
    ReactiveFormsModule
  ],
  templateUrl: './profile-field.component.html'
})
export class ProfileFieldComponent implements OnInit, OnChanges {
  @ViewChild('inputField') set inputFieldRef(input: ElementRef | null) {
    if (input && this.editing) {
      input.nativeElement.focus();
    }
  }

  @Input() fieldLabel!: string;
  @Input() httpFieldName!: string;
  @Input() fieldValue?: string;
  @Input() fieldValueVisualizer?: string | null;
  @Input() inputType!: string;
  @Input() additionalValidators: ValidatorFn[] = [];
  @Input() fieldTransformation?: (input: string) => string;
  @Output() editResult = new EventEmitter<string | null>();

  protected editing = false;
  protected waitingResponse = false;

  protected formField = new FormControl<string | null>(null);

  constructor(private http: HttpClient) {
  }

  ngOnInit() {
    // Fields annotated with @Input() are not reactive, so we need to manually set the value
    this.formField = new FormControl<string>(this.fieldValue || '', [Validators.required, ...this.additionalValidators]);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['fieldValue'] && changes['fieldValue'].currentValue !== undefined) {
      this.formField.setValue(changes['fieldValue'].currentValue);
    }
  }

  enableEditField(): void {
    this.editing = true;
  }

  editField(): void {
    if (this.formField.invalid) {
      console.warn("Trying to submit with invalid data")
      this.editing = false;
      return;
    }

    if (this.fieldValue != this.formField.value) {
      this.editing = false;
      return;
    }

    if (!this.waitingResponse) {
      this.waitingResponse = true;

      const body = {
        [this.httpFieldName]: this.fieldTransformation ? this.fieldTransformation(this.formField.value!) : this.formField.value!
      };

      this.http.patch(APP_CONSTANTS.ENDPOINT_USER_UPDATE, body, {
        withCredentials: true,
        responseType: 'json'
      }).subscribe({
        next: () => {
          this.editResult.emit(null);
          this.editing = false;
          this.waitingResponse = false;
          this.formField.setValue(this.fieldValue || '');
        },
        error: (httpErrorResponse: HttpErrorResponse) => {
          this.editResult.emit(httpErrorResponse?.error?.error || APP_CONSTANTS.MESSAGE_GENERIC_ERROR);
          this.editing = false;
          this.waitingResponse = false;
          this.formField.setValue(this.fieldValue || '');
        }
      });
    }
  }

  protected readonly ValidationUtils = ValidationUtils;
}
