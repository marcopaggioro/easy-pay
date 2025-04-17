import {Component} from '@angular/core';
import {Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {AuthorizationUtils} from '../utilities/authorization-utils';
import {WebSocketService} from '../utilities/web-socket.service';
import {NgbCollapse} from '@ng-bootstrap/ng-bootstrap';
import {HttpClient} from '@angular/common/http';
import {CookieService} from 'ngx-cookie-service';

@Component({
  selector: 'app-dashboard',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    NgbCollapse
  ],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
  mobileNavBarCollapsed = true;

  constructor(protected router: Router,
              protected http: HttpClient,
              protected cookieService: CookieService,
              protected webSocketService: WebSocketService) {
  }

  protected readonly AuthorizationUtils = AuthorizationUtils;
}
