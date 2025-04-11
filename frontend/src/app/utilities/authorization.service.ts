import {Injectable} from '@angular/core';
import {catchError, map, Observable, of} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import CryptoJS from 'crypto-js';
import {Router} from '@angular/router';
import {APP_CONSTANTS} from '../app.constants';


@Injectable({
  providedIn: 'root'
})
export class AuthorizationService {

  constructor(private http: HttpClient, private router: Router) {
  }

  hashPassword(password: string): string {
    return CryptoJS.SHA512(password).toString();
  }

  register(firstName: string, lastName: string, birthDate: string, email: string, password: string): Observable<string> {
    const body = {firstName, lastName, birthDate, email, encryptedPassword: this.hashPassword(password)};
    return this.http.post<string>(APP_CONSTANTS.USER_REGISTER_ENDPOINT, body, {withCredentials: true});
  }

  login(email: string, password: string): Observable<void> {
    return this.http.post<void>(APP_CONSTANTS.USER_LOGIN_ENDPOINT, {email, encryptedPassword: this.hashPassword(password)}, {
      withCredentials: true
    });
  }

  logout(): void {
    this.http.post(APP_CONSTANTS.USER_LOGOUT_ENDPOINT, {}, {withCredentials: true, responseType: 'text'}).subscribe(
      () => this.router.navigate([APP_CONSTANTS.PATH_ROOT])
    )
  }

  isAlreadyLoggedIn(): Observable<boolean> {
    return this.http.get(APP_CONSTANTS.USER_LOGIN_CHECK_ENDPOINT, {
      withCredentials: true,
      responseType: 'text'
    }).pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }

  redirectIfAlreadyLoggedIn(notLoggedCallback: () => void): void {
    this.isAlreadyLoggedIn().subscribe(
      isLogged => {
        if (isLogged) {
          this.router.navigate([APP_CONSTANTS.PATH_DASHBOARD]);
        } else {
          notLoggedCallback()
        }
      }
    )
  }

  redirectIfNotLoggedIn(): void {
    this.isAlreadyLoggedIn().subscribe(
      isLogged => {
        if (!isLogged) {
          this.router.navigate([APP_CONSTANTS.PATH_LOGIN]);
        }
      }
    )
  }
}
