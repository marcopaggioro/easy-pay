import {Injectable} from '@angular/core';
import {WebSocketSubject, WebSocketSubjectConfig} from 'rxjs/internal/observable/dom/WebSocketSubject';
import {webSocket} from 'rxjs/webSocket';
import {Observable, retry, shareReplay, timer} from 'rxjs';
import {APP_CONSTANTS} from '../app.constants';
import {CookieService} from 'ngx-cookie-service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private socket$!: WebSocketSubject<any>;
  private messages$!: Observable<any>;

  constructor(private cookieService: CookieService) {
  }

  createWebSocketConnection() {
    const customerId: string = this.cookieService.get(APP_CONSTANTS.CUSTOMER_ID_COOKIE_NAME);
    if (customerId) {
      const webSocketConfig: WebSocketSubjectConfig<any> = {
        url: APP_CONSTANTS.ENDPOINT_WS,
        openObserver: {
          next: () => console.log("[WS] Connection established")
        },
      };

      this.socket$ = webSocket(webSocketConfig);
      this.messages$ = this.socket$.pipe(
        retry({
          count: Infinity,
          delay: (_, retryAttempt) => {
            console.log(`[WS] Reconnect attempt ${retryAttempt}`);
            return timer(APP_CONSTANTS.INTERVAL_WS_RETRY);
          }
        }),
        shareReplay({bufferSize: 1, refCount: true})
      );
    }
  }

  getWebSocketMessages(): Observable<any> {
    if (!this.socket$ || this.socket$.closed) {
      this.createWebSocketConnection()
    }
    return this.messages$;
  }

  close(): void {
    if (this.socket$) {
      console.log("[WS] Closing connection")
      this.socket$.complete();
      this.socket$.unsubscribe();
    }
  }

}
