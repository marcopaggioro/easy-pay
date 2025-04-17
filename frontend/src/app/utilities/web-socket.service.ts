import {Injectable} from '@angular/core';
import {WebSocketSubject, WebSocketSubjectConfig} from 'rxjs/internal/observable/dom/WebSocketSubject';
import {webSocket} from 'rxjs/webSocket';
import {BehaviorSubject, Observable, retry, shareReplay, throwError, timer} from 'rxjs';
import {APP_CONSTANTS} from '../app.constants';
import {CookieService} from 'ngx-cookie-service';
import {Router} from '@angular/router';
import {WebSocketMessage} from '../classes/WebSocketMessage';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private socket$!: WebSocketSubject<WebSocketMessage>;
  private messageSubject$ = new BehaviorSubject<WebSocketMessage | null>(null);

  constructor(private cookieService: CookieService, private router: Router) {
  }

  createWebSocketConnection() {
    const customerId: string = this.cookieService.get(APP_CONSTANTS.CUSTOMER_ID_COOKIE_NAME);
    if (customerId) {
      const webSocketConfig: WebSocketSubjectConfig<WebSocketMessage> = {
        url: APP_CONSTANTS.ENDPOINT_WS,
        openObserver: {
          next: () => console.log("[WS] Connection established"),

        },
        closeObserver: {
          next: () => console.log("[WS] Connection closed")
        }
      };

      // Close previous connection
      if (this.socket$ && !this.socket$.closed) {
        this.socket$.complete();
      }

      // Create new connection
      this.socket$ = webSocket<WebSocketMessage>(webSocketConfig);
      this.socket$.pipe(
        retry({
            count: Infinity,
            delay: (_, retryAttempt) => {
              const cookieExists: boolean = this.cookieService.check(APP_CONSTANTS.CUSTOMER_ID_COOKIE_NAME);
              if (cookieExists) {
                console.log(`[WS] Reconnect attempt ${retryAttempt}`);
                return timer(APP_CONSTANTS.INTERVAL_WS_RETRY);
              } else {
                this.close();
                this.router.navigate([APP_CONSTANTS.PATH_ROOT])
                return throwError(() => new Error("Token expired"));
              }
            }
          }
        ),
        shareReplay({bufferSize: 1, refCount: true})
      ).subscribe({
        next: (message) => this.messageSubject$.next(message),
        error: (error) => console.error("[WS] Error", error)
      });
    }
  }

  getWebSocketMessages(): Observable<WebSocketMessage | null> {
    if (!this.socket$ || this.socket$.closed) {
      this.createWebSocketConnection();
    }
    return this.messageSubject$.asObservable();
  }

  close(): void {
    if (this.socket$) {
      this.socket$.complete();
      this.socket$.unsubscribe();
    }
  }

}
