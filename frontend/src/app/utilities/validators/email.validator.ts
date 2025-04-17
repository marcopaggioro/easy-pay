import {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

export function emailValidator(): ValidatorFn {
  // https://www.regular-expressions.info/email.html
  const regex = new RegExp('\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b', 'i');

  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }
    return regex.test(control.value) ? null : {invalidField: {value: control.value}};
  };
}
