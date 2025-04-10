import {Component} from '@angular/core';
import {Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {SpinnerComponent} from '../utilities/spinner.component';
import {HttpClient} from '@angular/common/http';

@Component({
  selector: 'app-dashboard',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    SpinnerComponent
  ],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {

  constructor(private http: HttpClient, private router: Router) {
  }

  logout(): void {
    this.http.post("http://localhost:9000/user/logout", {}, {withCredentials: true, responseType: 'text'}).subscribe(
      () => this.router.navigate(["/"])
    )
  }

}
