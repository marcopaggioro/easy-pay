import {catchError, map, Observable, of, tap} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import CryptoJS from 'crypto-js';
import {Router} from '@angular/router';
import {APP_CONSTANTS} from '../app.constants';
import {CookieService} from 'ngx-cookie-service';
import {WebSocketService} from './web-socket.service';
import {AuthorizationResponse} from '../classes/AuthorizationResponse';
import {UserDataService} from './user-data.service';


export class AuthorizationUtils {

  static hashPassword(password: string): string {
    return CryptoJS.SHA512(password).toString();
  }

  static getCustomerIdCookie(cookieService: CookieService): string {
    return cookieService.get(APP_CONSTANTS.CUSTOMER_ID_COOKIE_NAME);
  }

  static setCustomerIdCookie(cookieService: CookieService, authorization: AuthorizationResponse): void {
    cookieService.set(APP_CONSTANTS.CUSTOMER_ID_COOKIE_NAME, authorization.customerId, {
      path: "/",
      expires: new Date(authorization.expiration)
    });
  }

  static register(http: HttpClient, userDataService: UserDataService, webSocketService: WebSocketService, cookieService: CookieService, firstName: string, lastName: string, birthDate: string, email: string, password: string): Observable<void> {
    const body = {firstName, lastName, birthDate, email, encryptedPassword: this.hashPassword(password)};
    return http.post<AuthorizationResponse>(APP_CONSTANTS.ENDPOINT_USER_REGISTER, body, {
      withCredentials: true,
      responseType: 'json'
    }).pipe(map(authorization => {
      this.setCustomerIdCookie(cookieService, authorization);
      userDataService.getUserData();
      webSocketService.createWebSocketConnection();
    }));
  }

  static login(http: HttpClient, userDataService: UserDataService, webSocketService: WebSocketService, cookieService: CookieService, email: string, password: string): Observable<void> {
    return http.post<AuthorizationResponse>(APP_CONSTANTS.ENDPOINT_USER_LOGIN, {
      email,
      encryptedPassword: this.hashPassword(password)
    }, {
      withCredentials: true,
      responseType: 'json'
    }).pipe(map(authorization => {
      this.setCustomerIdCookie(cookieService, authorization);
      userDataService.getUserData();
      webSocketService.createWebSocketConnection();
    }));
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

  static isLoggedIn(http: HttpClient): Observable<boolean> {
    return http.get(APP_CONSTANTS.ENDPOINT_USER_LOGIN_CHECK, {
      withCredentials: true,
      responseType: 'text'
    }).pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }

  static redirectIfLoggedIn(router: Router, http: HttpClient): void {
    this.isLoggedIn(http).subscribe(
      isLogged => {
        if (isLogged) {
          router.navigate([APP_CONSTANTS.PATH_DASHBOARD]);
        }
      }
    )
  }

  static redirectIfNotLoggedIn(router: Router, http: HttpClient): void {
    this.isLoggedIn(http).subscribe(
      isLogged => {
        if (!isLogged) {
          router.navigate([APP_CONSTANTS.PATH_LOGIN]);
        }
      }
    )
  }
}
