import {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

// otherValue in wrapped in a function to have always the latest value
export function notEqualsToValidator(otherValue: () => string): ValidatorFn {

  return (control: AbstractControl): ValidationErrors | null => {
    if (control.value && otherValue() && control.value !== otherValue()) {
      return null;
    } else {
      return {invalidField: {value: control.value}};
    }
  };
}
