import {catchError, EMPTY, Observable, tap} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import CryptoJS from 'crypto-js';
import {Router} from '@angular/router';
import {APP_CONSTANTS} from '../app.constants';
import {CookieService} from 'ngx-cookie-service';
import {WebSocketService} from './web-socket.service';


export class AuthorizationUtils {

  static hashPassword(password: string): string {
    return CryptoJS.SHA512(password).toString();
  }

  static register(http: HttpClient, firstName: string, lastName: string, birthDate: string, email: string, password: string): Observable<void> {
    const body = {firstName, lastName, birthDate, email, encryptedPassword: this.hashPassword(password)};
    return http.post<void>(APP_CONSTANTS.ENDPOINT_USER_REGISTER, body, {withCredentials: true});
  }

  static login(http: HttpClient, email: string, password: string): Observable<void> {
    return http.post<void>(APP_CONSTANTS.ENDPOINT_USER_LOGIN, {
      email,
      encryptedPassword: this.hashPassword(password)
    }, {withCredentials: true});
  }

  static refreshToken(http: HttpClient): Observable<void> {
    console.log("Refreshing token")
    return http.post<void>(APP_CONSTANTS.ENDPOINT_USER_REFRESH_TOKEN, {}, {
      withCredentials: true,
      responseType: 'json'
    }).pipe(tap(() => console.log("[WS] Token refreshed")));
  }

  static logout(http: HttpClient, cookieService: CookieService, router: Router, webSocketService: WebSocketService): void {
    http.post(APP_CONSTANTS.ENDPOINT_USER_LOGOUT, {}, {withCredentials: true, responseType: 'text'}).subscribe(
      () => {
        cookieService.delete(APP_CONSTANTS.CUSTOMER_ID_COOKIE_NAME);
        webSocketService.close();
        router.navigate([APP_CONSTANTS.PATH_ROOT]);
      }
    )
  }

  static checkLogin(http: HttpClient): Observable<void> {
    return http.get<void>(APP_CONSTANTS.ENDPOINT_USER_LOGIN_CHECK, {withCredentials: true});
  }

  static redirectIfLoggedIn(router: Router, http: HttpClient): void {
    this.checkLogin(http).pipe(
      tap(() => router.navigate([APP_CONSTANTS.PATH_DASHBOARD])),
      catchError(() => EMPTY)
    ).subscribe();
  }
}
