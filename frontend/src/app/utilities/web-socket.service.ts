import {Injectable} from '@angular/core';
import {WebSocketSubject} from 'rxjs/internal/observable/dom/WebSocketSubject';
import {webSocket} from 'rxjs/webSocket';
import {Observable} from 'rxjs';
import {APP_CONSTANTS} from '../app.constants';
import {CookieService} from 'ngx-cookie-service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private socket$!: WebSocketSubject<any>;

  constructor(private cookieService: CookieService) {
  }

  // TODO ricreare sessione se si chiude?
  getWebSocketMessages(): Observable<any> {
    const customerId: string = this.cookieService.get(APP_CONSTANTS.CUSTOMER_ID_COOKIE_NAME);
    const url = `${APP_CONSTANTS.ENDPOINT_WS}/${customerId}`;
    this.socket$ = webSocket(url);
    return this.socket$.asObservable();
  }

  close(): void {
    if (this.socket$) {
      this.socket$.complete();
    }
  }

}
