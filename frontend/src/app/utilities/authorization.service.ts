import {Injectable} from '@angular/core';
import {catchError, map, Observable, of} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import CryptoJS from 'crypto-js';
import {Router} from '@angular/router';
import {APP_CONSTANTS} from '../app.constants';
import {CookieService} from 'ngx-cookie-service';
import {WebSocketService} from './web-socket.service';


@Injectable({
  providedIn: 'root'
})
export class AuthorizationService {

  constructor(private http: HttpClient, private router: Router, private cookieService: CookieService) {
  }

  hashPassword(password: string): string {
    return CryptoJS.SHA512(password).toString();
  }

  setCustomerIdCookie(customerId: string): void {
    this.cookieService.set(APP_CONSTANTS.CUSTOMER_ID_COOKIE_NAME, customerId);
  }

  register(firstName: string, lastName: string, birthDate: string, email: string, password: string): Observable<void> {
    const body = {firstName, lastName, birthDate, email, encryptedPassword: this.hashPassword(password)};
    return this.http.post<string>(APP_CONSTANTS.ENDPOINT_USER_REGISTER, body, {
      withCredentials: true,
      responseType: 'json'
    }).pipe(map(customerId => this.setCustomerIdCookie(customerId)));
  }

  login(email: string, password: string): Observable<void> {
    return this.http.post<string>(APP_CONSTANTS.ENDPOINT_USER_LOGIN, {
      email,
      encryptedPassword: this.hashPassword(password)
    }, {
      withCredentials: true,
      responseType: 'json'
    }).pipe(map(customerId => this.setCustomerIdCookie(customerId)));
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

  isAlreadyLoggedIn(): Observable<boolean> {
    return this.http.get(APP_CONSTANTS.ENDPOINT_USER_LOGIN_CHECK, {
      withCredentials: true,
      responseType: 'text'
    }).pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }

  redirectIfAlreadyLoggedIn(): void {
    this.isAlreadyLoggedIn().subscribe(
      isLogged => {
        if (isLogged) {
          this.router.navigate([APP_CONSTANTS.PATH_DASHBOARD]);
        }
      }
    )
  }
}
