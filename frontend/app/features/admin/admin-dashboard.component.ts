import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="dashboard-header">
      <div>
        <h2>Contract Administrator Dashboard</h2>
        <p>Welcome, <strong>{{ authService.currentUser()?.fullName }}</strong></p>
      </div>
      <div class="sub-nav">
        <a routerLink="/admin/add-contract" routerLinkActive="active" class="nav-pill">Load Contract</a>
        <a routerLink="/admin/contracts" routerLinkActive="active" class="nav-pill">View Contracts</a>
        <a routerLink="/admin/search-contracts" routerLinkActive="active" class="nav-pill">Search Contracts</a>
      </div>
    </div>
    <router-outlet></router-outlet>
  `,
  styles: [`
    .dashboard-header {
      background: white;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.08);
      margin-bottom: 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 15px;
    }
    .dashboard-header h2 { margin: 0; color: #004b87; }
    .dashboard-header p { margin: 4px 0 0; color: #555; }
    .sub-nav { display: flex; gap: 10px; flex-wrap: wrap; }
    .nav-pill {
      text-decoration: none;
      padding: 9px 16px;
      border-radius: 5px;
      background: #e6f0fa;
      color: #004b87;
      font-weight: 600;
    }
    .nav-pill.active, .nav-pill:hover {
      background: #004b87;
      color: white;
    }
  `]
})
export class AdminDashboardComponent {
  // Dashboard links are role-protected by app.routes.ts and navigate to child views.
  constructor(public authService: AuthService) {}
}
