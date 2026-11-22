import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  template: `
    <div class="layout">
      <nav *ngIf="authService.isLoggedIn()">
        <h1>Sun Travels</h1>
        <div class="user-controls">
          <span class="role-tag">
            {{ authService.getRole() === 'CONTRACT_ADMIN' ? 'Contract Administrator' : 'Call Center Agent' }}
          </span>
          <button type="button" class="logout-btn" (click)="authService.logout()">Log Out</button>
        </div>
      </nav>
      <main>
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    nav {
      background: #004b87;
      color: white;
      padding: 15px 30px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    nav h1 { margin: 0; font-size: 22px; }
    .user-controls { display: flex; align-items: center; gap: 15px; }
    .role-tag {
      background: rgba(255, 255, 255, 0.2);
      padding: 5px 12px;
      border-radius: 14px;
      font-size: 13px;
      font-weight: 600;
    }
    .logout-btn {
      background: #d9534f;
      color: white;
      border: none;
      padding: 7px 14px;
      border-radius: 4px;
      cursor: pointer;
      font-weight: 600;
    }
    .logout-btn:hover { background: #c9302c; }
    main { padding: 30px; max-width: 1200px; margin: 0 auto; }
  `]
})
export class AppComponent {
  constructor(public authService: AuthService) {}
}
