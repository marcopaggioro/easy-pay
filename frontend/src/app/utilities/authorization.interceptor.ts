import {HttpClient, HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import {catchError, switchMap, throwError} from 'rxjs';
import {inject} from '@angular/core';
import {Router} from '@angular/router';
import {APP_CONSTANTS} from '../app.constants';
import {AuthorizationUtils} from './authorization-utils';

export const authorizationInterceptor: HttpInterceptorFn = (request, next) => {
  const http = inject(HttpClient);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !request.url.includes(APP_CONSTANTS.ENDPOINT_USER_LOGIN_CHECK) && !request.url.includes(APP_CONSTANTS.ENDPOINT_USER_LOGIN)) {
        if (!request.url.includes(APP_CONSTANTS.ENDPOINT_USER_REFRESH_TOKEN)) {
          return AuthorizationUtils.refreshToken(http).pipe(
            switchMap(() => {
              console.log(`Performing again same request (${request.method} ${request.url})`);
              return next(request)
            })
          );
        } else {
          router.navigate([APP_CONSTANTS.PATH_LOGIN]);
          return throwError(() => error);
        }
      } else {
        return throwError(() => error);
      }
    })
  );
};
