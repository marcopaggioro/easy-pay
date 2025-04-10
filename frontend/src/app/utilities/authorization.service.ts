import {Injectable} from '@angular/core';
import {catchError, map, Observable, of} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import CryptoJS from 'crypto-js';
import {Router} from '@angular/router';


@Injectable({
  providedIn: 'root'
})
export class AuthorizationService {

  //TODO mettere da qualche parte l'endpoint di base
  //TODO mettere da qualche parte gli endpoints

  constructor(private http: HttpClient, private router: Router) {
  }

  hashPassword(password: string): string {
    return CryptoJS.SHA256(password).toString();
  }

  register(name: string, surname: string, birthDate: string, email: string, password: string): Observable<string> {
    const body = {name, surname, birthDate, email, encryptedPassword: this.hashPassword(password)};
    return this.http.post<string>("http://localhost:9000/user", body);
  }

  login(email: string, password: string): Observable<void> {
    return this.http.post<void>("http://localhost:9000/user/login", {email, encryptedPassword: this.hashPassword(password)}, {
      withCredentials: true
    });
  }

  isAlreadyLoggedIn(): Observable<boolean> {
    return this.http.get("http://localhost:9000/user/login/check", {
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
          this.router.navigate(["dashboard"]);
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
          this.router.navigate(["/login"]);
        }
      }
    )
  }
}
