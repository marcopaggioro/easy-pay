import {Injectable, OnInit} from '@angular/core';
import {UserData} from '../classes/UserData';
import {APP_CONSTANTS} from '../app.constants';
import {HttpClient} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class UserdataService {
  userData!: UserData;

  constructor(private http: HttpClient) {
    this.getUserData();
  }

  getUserData(): void {
    this.http.get<UserData>(APP_CONSTANTS.USER_GET_ENDPOINT, {withCredentials: true, responseType: 'json'}).subscribe(
      userData => this.userData = userData
    );
  }
}
