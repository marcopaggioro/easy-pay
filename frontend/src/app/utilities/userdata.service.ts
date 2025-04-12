import {Injectable} from '@angular/core';
import {UserData} from '../classes/UserData';
import {APP_CONSTANTS} from '../app.constants';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, tap} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserdataService {
  private userDataSubject = new BehaviorSubject<UserData | null>(null);
  userData$ = this.userDataSubject.asObservable();

  constructor(private http: HttpClient) {
    this.getUserData();
  }

  private getUserData(): void {
    this.http.get<UserData>(APP_CONSTANTS.USER_GET_ENDPOINT, {withCredentials: true, responseType: 'json'})
      .pipe(
        tap(userData => this.userDataSubject.next(userData))
      )
      .subscribe();
  }
}
