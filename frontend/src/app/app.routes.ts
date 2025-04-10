import {Routes} from '@angular/router';
import {RegisterComponent} from './register/register.component';
import {LoginComponent} from './login/login.component';
import {DashboardComponent} from './dashboard/dashboard.component';

export const routes: Routes = [
  {path: "register", component: RegisterComponent},
  {path: "login", component: LoginComponent},
  {path: "dashboard", component: DashboardComponent},

  //TODO cos'è?
  {path: "", redirectTo: "login", pathMatch: "full"},
  // TODO {path: "**", component: ErrorNotFoundComponent, title: "Areson - Pagina non trovata"}
];
