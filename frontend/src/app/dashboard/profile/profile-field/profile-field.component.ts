import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
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
export class ProfileFieldComponent implements OnInit {
  @Input() fieldLabel!: string;
  @Input() httpFieldName!: string;
  @Input() fieldValue!: any;
  @Input() fieldPlaceholder!: string;
  @Input() inputType!: string;
  @Input() additionalValidators: ValidatorFn[] = [];

  @Output() editResult = new EventEmitter<string | null>();

  editing: boolean = false;
  waitingResponse: boolean = false;

  formField !: FormControl;

  constructor(private http: HttpClient) {
  }

  ngOnInit() {
    this.formField = new FormControl(this.fieldValue, [Validators.required, ...this.additionalValidators]);
  }

  enableEditField(): void {
    this.editing = true;
  }

  editField(): void {
    this.waitingResponse = true;

    const body = {[this.httpFieldName]: this.formField.value!}
    this.http.patch(APP_CONSTANTS.USER_UPDATE_ENDPOINT, body, {withCredentials: true, responseType: 'json'}).subscribe({
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
