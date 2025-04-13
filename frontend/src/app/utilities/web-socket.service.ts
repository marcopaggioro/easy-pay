import {Injectable, NgZone} from '@angular/core';
import {WebSocketSubject} from 'rxjs/internal/observable/dom/WebSocketSubject';
import {webSocket} from 'rxjs/webSocket';
import {delayWhen, Observable, retry, tap, timer} from 'rxjs';
import {APP_CONSTANTS} from '../app.constants';
import {CookieService} from 'ngx-cookie-service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private readonly HEARTBEAT_INTERVAL = 30000;
  private socket$!: WebSocketSubject<any>;

  constructor(private cookieService: CookieService, private ngZone: NgZone) {
    const customerId: string = this.cookieService.get(APP_CONSTANTS.CUSTOMER_ID_COOKIE_NAME);
    const url = `${APP_CONSTANTS.ENDPOINT_WS}/${customerId}`;
    this.socket$ = webSocket(url);

    this.ngZone.runOutsideAngular(() => {
      timer(this.HEARTBEAT_INTERVAL, this.HEARTBEAT_INTERVAL).subscribe(
        () => this.socket$.next({})
      );
    });
  }

  getWebSocketMessages(): Observable<any> {
    return this.socket$.asObservable().pipe(
      tap({
        error: err => console.error('WebSocket error:', err)
      }),
      retry({
        count: Infinity, // oppure un numero fisso di tentativi, ad esempio 10
        delay: (error, retryAttempt) => {
          //TODO migliorare log e mettere anche log di successo
          console.log(`Tentativo di riconnessione ${retryAttempt}: attendo 5 secondi...`);
          return timer(5000); // attende 5 secondi prima del retry
        }
      })
    );
  }

  close(): void {
    if (this.socket$) {
      this.socket$.complete();
    }
  }

}
