import {Injectable} from '@angular/core';
import {WebSocketSubject, WebSocketSubjectConfig} from 'rxjs/internal/observable/dom/WebSocketSubject';
import {webSocket} from 'rxjs/webSocket';
import {BehaviorSubject, catchError, mergeMap, Observable, of, retry, shareReplay, throwError, timer} from 'rxjs';
import {APP_CONSTANTS} from '../app.constants';
import {Router} from '@angular/router';
import {WebSocketMessage} from '../classes/WebSocketMessage';
import {AuthorizationUtils} from './authorization-utils';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private socket$!: WebSocketSubject<WebSocketMessage>;
  private messageSubject$ = new BehaviorSubject<WebSocketMessage | null>(null);

  constructor(private router: Router, private http: HttpClient) {
  }

  createWebSocketConnection() {
    const webSocketConfig: WebSocketSubjectConfig<WebSocketMessage> = {
      url: APP_CONSTANTS.ENDPOINT_WS,
      openObserver: {
        next: () => console.log("[WS] Connection established")
      },
      closeObserver: {
        next: () => console.log("[WS] Connection closed")
      }
    };

    // Close previous connection
    if (this.socket$ && !this.socket$.closed) {
      this.socket$.complete();
    }

    // Create a new connection
    this.socket$ = webSocket<WebSocketMessage>(webSocketConfig);
    this.socket$.pipe(
      retry({
        count: Infinity,
        delay: (_, retryAttempt) => {
          console.log(`[WS] Connection failed, retry attempt ${retryAttempt}`);

          return AuthorizationUtils.checkLogin(this.http).pipe(
            // Still logged in
            mergeMap(() => of(undefined)),

            // Failed to check login
            catchError((checkError: HttpErrorResponse) => {
              // Check login 401: try refreshing token
              if (checkError.status === 401) {
                return AuthorizationUtils.refreshToken(this.http).pipe(
                  // Token refreshed
                  mergeMap(() => of(undefined)),
                  // Failed to refresh token
                  catchError((refreshError: HttpErrorResponse) => {
                    if (refreshError.status === 401) {
                      this.router.navigate([APP_CONSTANTS.PATH_LOGIN]);
                      return throwError(() => refreshError);
                    }
                    // Server offline
                    return timer(APP_CONSTANTS.INTERVAL_WS_RETRY);
                  })
                );
              }

              // Server offline
              return timer(APP_CONSTANTS.INTERVAL_WS_RETRY);
            })
          );
        }
      }),
      shareReplay({bufferSize: 1, refCount: true})
    ).subscribe({
      next: (message) => this.messageSubject$.next(message),
      error: (error) => console.error("[WS] Error", error)
    });
  }

  getWebSocketMessages(): Observable<WebSocketMessage | null> {
    return this.messageSubject$.asObservable();
  }

  close(): void {
    if (this.socket$) {
      this.socket$.complete();
      this.socket$.unsubscribe();
    }
  }

}
