import {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

export function noNumbersValidator(): ValidatorFn {
  const regex = new RegExp('^[^0-9]*$');

  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }
    return regex.test(control.value) ? null : {invalidField: "Non sono consentiti numeri"};
  };
}
