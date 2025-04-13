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
  private readonly socket$!: WebSocketSubject<any>;
  private readonly messages$!: Observable<any>;

  constructor(private cookieService: CookieService) {
    const customerId: string = this.cookieService.get(APP_CONSTANTS.CUSTOMER_ID_COOKIE_NAME);
    if (customerId) {
      const url: string = `${APP_CONSTANTS.ENDPOINT_WS}/${customerId}`;
      const webSocketConfig: WebSocketSubjectConfig<any> = {
        url,
        openObserver: {
          next: () => console.log("[WS] Connessione stabilita")
        }
      };

      this.socket$ = webSocket(webSocketConfig);
      this.messages$ = this.socket$.pipe(
        retry({
          count: Infinity,
          delay: (_, retryAttempt) => {
            console.log(`[WS] Tentativo di riconnessione ${retryAttempt}`);
            return timer(APP_CONSTANTS.INTERVAL_WS_RETRY);
          }
        }),
        shareReplay({bufferSize: 1, refCount: true})
      );
    }
  }

  getWebSocketMessages(): Observable<any> {
    return this.messages$;
  }

  close(): void {
    if (this.socket$) {
      this.socket$.complete();
    }
  }

}
