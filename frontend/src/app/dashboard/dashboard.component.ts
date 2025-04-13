import {Component} from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {AuthorizationService} from '../utilities/authorization.service';
import {WebSocketService} from '../utilities/web-socket.service';
import {NgbCollapse} from '@ng-bootstrap/ng-bootstrap';

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

  constructor(protected authorizationService: AuthorizationService, protected webSocketService: WebSocketService) {
  }
}
