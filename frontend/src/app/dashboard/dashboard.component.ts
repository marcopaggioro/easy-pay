import {Component} from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {AuthorizationService} from '../utilities/authorization.service';
import {WebSocketService} from '../utilities/web-socket.service';

@Component({
  selector: 'app-dashboard',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {

  constructor(protected authorizationService: AuthorizationService, protected webSocketService: WebSocketService) {
  }
}
