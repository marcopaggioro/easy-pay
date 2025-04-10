import {HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import {catchError, throwError} from 'rxjs';
import {inject} from '@angular/core';
import {Router} from '@angular/router';

export const authorizationInterceptor: HttpInterceptorFn = (request, next) => {
  const router = inject(Router);

  //TODO test
  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      //TODO path parametrizzato
      console.log(request.url)
      if (error.status === 401 && !request.url.includes("login/check")) {
        router.navigate(["/login"]);
        return throwError(() => error);
      } else {
        return throwError(() => error);
      }
    })
  );

};
