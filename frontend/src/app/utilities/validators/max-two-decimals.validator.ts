import {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

export function maxTwoDecimalsValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (value === null || value === undefined || value === '') {
      return null;
    }

    const valueStr = value.toString().replace(',', '.');

    const parts = valueStr.split('.');
    if (parts.length === 2) {
      const decimals = parts[1];
      if (decimals.length > 2) {
        return {invalidField: value};
      }
    }

    return null;
  };
}
