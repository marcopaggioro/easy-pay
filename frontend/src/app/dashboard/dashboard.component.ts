import {Component, OnDestroy, OnInit} from '@angular/core';
import {Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {AuthorizationUtils} from '../utilities/authorization-utils';
import {WebSocketService} from '../utilities/web-socket.service';
import {NgbCollapse} from '@ng-bootstrap/ng-bootstrap';
import {HttpClient} from '@angular/common/http';
import {UserDataService} from '../utilities/user-data.service';

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
export class DashboardComponent implements OnInit, OnDestroy {
  mobileNavBarCollapsed = true;

  constructor(protected router: Router,
              protected http: HttpClient,
              private userDataService: UserDataService,
              protected webSocketService: WebSocketService) {
  }

  ngOnInit() {
    this.webSocketService.createWebSocketConnection();
    this.userDataService.getUserDataIfNonEmpty();
  }

  ngOnDestroy(): void {
    this.webSocketService.close();
  }

  protected readonly AuthorizationUtils = AuthorizationUtils;
}
