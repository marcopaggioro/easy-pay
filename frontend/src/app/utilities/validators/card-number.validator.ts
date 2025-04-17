import {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

export function cardNumberValidator(): ValidatorFn {
  const regex = new RegExp('^\\d{16}$');

  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }
    return regex.test(control.value) ? null : {invalidCardNumber: {value: control.value}};
  };
}
