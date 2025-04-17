import {Injectable} from '@angular/core';
import {catchError, map, Observable, of} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import CryptoJS from 'crypto-js';
import {Router} from '@angular/router';
import {APP_CONSTANTS} from '../app.constants';
import {CookieService} from 'ngx-cookie-service';
import {WebSocketService} from './web-socket.service';
import {Authorization} from '../classes/Authorization';
import {UserDataService} from './user-data.service';


@Injectable({
  providedIn: 'root'
})
export class AuthorizationService {

  constructor(private http: HttpClient, private router: Router, private cookieService: CookieService, private webSocketService: WebSocketService, private userDataService: UserDataService) {
  }

  hashPassword(password: string): string {
    return CryptoJS.SHA512(password).toString();
  }

  getCustomerIdCookie(): string {
    return this.cookieService.get(APP_CONSTANTS.CUSTOMER_ID_COOKIE_NAME);
  }

  setCustomerIdCookie(authorization: Authorization): void {
    this.cookieService.set(APP_CONSTANTS.CUSTOMER_ID_COOKIE_NAME, authorization.customerId, {
      path: "/",
      expires: new Date(authorization.expiration)
    });
  }

  register(firstName: string, lastName: string, birthDate: string, email: string, password: string): Observable<void> {
    const body = {firstName, lastName, birthDate, email, encryptedPassword: this.hashPassword(password)};
    return this.http.post<Authorization>(APP_CONSTANTS.ENDPOINT_USER_REGISTER, body, {
      withCredentials: true,
      responseType: 'json'
    }).pipe(map(authorization => {
      this.setCustomerIdCookie(authorization);
      this.userDataService.getUserData();
      this.webSocketService.createWebSocketConnection();
    }));
  }

  login(email: string, password: string): Observable<void> {
    return this.http.post<Authorization>(APP_CONSTANTS.ENDPOINT_USER_LOGIN, {
      email,
      encryptedPassword: this.hashPassword(password)
    }, {
      withCredentials: true,
      responseType: 'json'
    }).pipe(map(authorization => {
      this.setCustomerIdCookie(authorization);
      this.userDataService.getUserData();
      this.webSocketService.createWebSocketConnection();
    }));
  }

  logout(webSocketService: WebSocketService): void {
    this.http.post(APP_CONSTANTS.ENDPOINT_USER_LOGOUT, {}, {withCredentials: true, responseType: 'text'}).subscribe(
      () => {
        this.cookieService.delete(APP_CONSTANTS.CUSTOMER_ID_COOKIE_NAME);
        webSocketService.close();
        this.router.navigate([APP_CONSTANTS.PATH_ROOT]);
      }
    )
  }

  isLoggedIn(): Observable<boolean> {
    return this.http.get(APP_CONSTANTS.ENDPOINT_USER_LOGIN_CHECK, {
      withCredentials: true,
      responseType: 'text'
    }).pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }

  redirectIfLoggedIn(): void {
    this.isLoggedIn().subscribe(
      isLogged => {
        if (isLogged) {
          this.router.navigate([APP_CONSTANTS.PATH_DASHBOARD]);
        }
      }
    )
  }
}
