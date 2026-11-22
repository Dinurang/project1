import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="dashboard-header">
      <div>
        <h2>Call Center Agent Dashboard</h2>
        <p>Welcome, <strong>{{ authService.currentUser()?.fullName }}</strong></p>
      </div>
      <div class="sub-nav">
        <a routerLink="/agent/search" routerLinkActive="active" class="nav-pill">Search Rooms</a>
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
    .nav-pill {
      text-decoration: none;
      padding: 9px 16px;
      border-radius: 5px;
      background: #004b87;
      color: white;
      font-weight: 600;
    }
  `]
})
export class AgentDashboardComponent {
  // The dashboard exposes the agent search child route and current profile name.
  constructor(public authService: AuthService) {}
}
