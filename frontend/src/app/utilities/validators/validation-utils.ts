import {FormControl, ValidatorFn, Validators} from '@angular/forms';
import {APP_CONSTANTS} from '../../app.constants';

export class ValidationUtils {

  static getMinAmountValidator(): ValidatorFn {
    return Validators.min(APP_CONSTANTS.MIN_AMOUNT);
  }

  static getMaxAmountValidator(): ValidatorFn {
    return Validators.max(APP_CONSTANTS.MAX_AMOUNT);
  }

  static printErrors(control: FormControl) {
    if (control.errors?.['required']) {
      return "Campo obbligatorio";
    } else if (control.errors?.['min']) {
      return `Quantità minima consentita di ${control.errors?.['min'].min}`;
    } else if (control.errors?.['max']) {
      return `Quantità massima consentita di ${control.errors?.['min'].max}`;
    } else if (control.errors?.['invalidField']) {
      return control.errors?.['invalidField'];
    } else if (control.errors?.['minlength']) {
      return `Sono richiesti almeno ${control.errors?.['minlength'].requiredLength} caratteri`;
    } else if (control.errors?.['maxlength']) {
      return `Sono consentiti al massimo ${control.errors?.['maxlength'].requiredLength} caratteri`;
    } else {
      return "Errore generico";
    }
  }

}
