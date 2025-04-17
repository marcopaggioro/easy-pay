import {HttpClient, HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import {catchError, switchMap, throwError} from 'rxjs';
import {inject} from '@angular/core';
import {Router} from '@angular/router';
import {APP_CONSTANTS} from '../app.constants';

export const authorizationInterceptor: HttpInterceptorFn = (request, next) => {
  const http = inject(HttpClient);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !request.url.includes(APP_CONSTANTS.ENDPOINT_USER_LOGIN_CHECK)) {
        if (!request.url.includes(APP_CONSTANTS.ENDPOINT_USER_REFRESH_TOKEN)) {
          console.log("Refreshing token");
          return http.post<void>(APP_CONSTANTS.ENDPOINT_USER_REFRESH_TOKEN, {}, {
            withCredentials: true,
            responseType: 'json'
          }).pipe(
            switchMap(() => {
              console.log(`Performing again same request (${request.url})`);
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
