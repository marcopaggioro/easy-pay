import {Routes} from '@angular/router';
import {RegisterComponent} from './register/register.component';
import {LoginComponent} from './login/login.component';
import {DashboardComponent} from './dashboard/dashboard.component';
import {WalletComponent} from './dashboard/wallet/wallet.component';
import {TransferComponent} from './dashboard/transfer/transfer.component';
import {RechargeComponent} from './dashboard/recharge/recharge.component';
import {ProfileComponent} from './dashboard/profile/profile.component';
import {HomepageComponent} from './homepage/homepage.component';

export const routes: Routes = [
  {path: "register", component: RegisterComponent},
  {path: "login", component: LoginComponent},
  {
    path: "dashboard", component: DashboardComponent, children: [
      {path: "", redirectTo: "wallet", pathMatch: "full"},
      {path: "wallet", component: WalletComponent},
      {path: "transfer", component: TransferComponent},
      {path: "recharge", component: RechargeComponent},
      {path: "profile", component: ProfileComponent}
    ]
  },
  {path: "", component: HomepageComponent},
  {path: "**", redirectTo: ""}
];
