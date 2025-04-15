import {Injectable} from '@angular/core';
import {UserData} from '../classes/UserData';
import {APP_CONSTANTS} from '../app.constants';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, tap} from 'rxjs';
import {WebSocketService} from './web-socket.service';

@Injectable()
export class UserDataService {
  private userDataSubject = new BehaviorSubject<UserData | null>(null);
  userData$ = this.userDataSubject.asObservable();

  constructor(private http: HttpClient, private webSocketService: WebSocketService) {
    this.getUserData();

    this.webSocketService.getWebSocketMessages().subscribe(
      (message) => {
        if (message?.type == APP_CONSTANTS.WS_USER_DATA_UPDATED) {
          this.getUserData();
        }
      }
    );
  }

  private getUserData(): void {
    this.http.get<UserData>(APP_CONSTANTS.ENDPOINT_USER_GET, {withCredentials: true, responseType: 'json'})
      .pipe(
        tap(userData => this.userDataSubject.next(userData))
      )
      .subscribe();
  }
}
