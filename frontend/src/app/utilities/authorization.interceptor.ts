import {HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import {catchError, throwError} from 'rxjs';
import {inject} from '@angular/core';
import {Router} from '@angular/router';
import {APP_CONSTANTS} from '../app.constants';

export const authorizationInterceptor: HttpInterceptorFn = (request, next) => {
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !request.url.includes("login/check")) {
        router.navigate([APP_CONSTANTS.PATH_LOGIN]);
        return throwError(() => error);
      } else {
        return throwError(() => error);
      }
    })
  );

};
