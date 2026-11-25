import { Routes } from '@angular/router';
import { AuthComponent } from './features/auth/auth.component';
import { AdminDashboardComponent } from './features/admin/admin-dashboard.component';
import { AgentDashboardComponent } from './features/agent/agent-dashboard.component';
import { ContractFormComponent } from './contract-form.component';
import { ContractListComponent } from './contract-list.component';
import { ContractManagementComponent } from './features/admin/contract-management.component';
import { SearchContractComponent } from './search-contract/search-contract.component';
import { SearchComponent } from './search.component';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'auth', pathMatch: 'full' },

  // Authentication guards prevent users from entering another role's dashboard.
  // Common First Page (Sign Up & Log In)
  {
    path: 'auth',
    component: AuthComponent,
    canActivate: [guestGuard]
  },

  // Contract Administrator Role-Separated URLs (/admin/*)
  {
    path: 'admin',
    component: AdminDashboardComponent,
    canActivate: [authGuard, roleGuard],
    data: { role: 'CONTRACT_ADMIN' },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', redirectTo: 'contracts', pathMatch: 'full' },
      { path: 'add-contract', component: ContractFormComponent },
      { path: 'contracts', component: ContractListComponent },
      { path: 'edit-contracts', component: ContractManagementComponent },
      { path: 'search-contracts', component: SearchContractComponent }
    ]
  },

  // Call Center Agent Role-Separated URLs (/agent/*)
  {
    path: 'agent',
    component: AgentDashboardComponent,
    canActivate: [authGuard, roleGuard],
    data: { role: 'CALL_CENTER_AGENT' },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', redirectTo: 'search', pathMatch: 'full' },
      { path: 'search', component: SearchComponent }
    ]
  },

  { path: '**', redirectTo: 'auth' }
];
