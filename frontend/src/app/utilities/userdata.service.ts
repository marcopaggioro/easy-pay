import {Injectable, Injector} from '@angular/core';
import {UserData} from '../classes/UserData';
import {APP_CONSTANTS} from '../app.constants';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, tap} from 'rxjs';
import {WebSocketService} from './web-socket.service';

@Injectable({
  providedIn: 'root'
})
export class UserdataService {
  //TODO capire bene come si interfaccia
  private userDataSubject = new BehaviorSubject<UserData | null>(null);
  userData$ = this.userDataSubject.asObservable();

  constructor(private http: HttpClient, private injector: Injector) {
    this.getUserData();

    // this.webSocketService.getWebSocketMessages().subscribe(
    //   (message) => {
    //     if (message.event == APP_CONSTANTS.WS_USER_DATA_UPDATED) {
    //       this.getUserData();
    //     }
    //   }
    // );
    //TODO funziona ma non sono convinto
    Promise.resolve().then(() => {
      const webSocketService = this.injector.get(WebSocketService);
      webSocketService.getWebSocketMessages().subscribe(message => {
        console.log("received " + message)
        if (message.event === APP_CONSTANTS.WS_USER_DATA_UPDATED) {
          this.getUserData();
        }
      });
    });
  }

  private getUserData(): void {
    this.http.get<UserData>(APP_CONSTANTS.ENDPOINT_USER_GET, {withCredentials: true, responseType: 'json'})
      .pipe(
        tap(userData => this.userDataSubject.next(userData))
      )
      .subscribe();
  }
}
