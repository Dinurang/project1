import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { LoginRequest, SignupRequest, UserRole } from '../../core/models/auth.model';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auth.component.html',
  styleUrls: ['./auth.component.css']
})
export class AuthComponent {
  // Login and signup share this page; the activeTab controls which form is shown.
  activeTab: 'login' | 'signup' = 'login';
  errorMessage = '';
  successMessage = '';
  isSubmitting = false;

  loginData: LoginRequest = {
    username: '',
    password: ''
  };

  signupData: SignupRequest = {
    username: '',
    email: '',
    password: '',
    role: 'CALL_CENTER_AGENT',
    fullName: '',
    agentCode: '',
    deskExtension: '',
    employeeCode: '',
    contactNo: ''
  };

  constructor(private authService: AuthService) {}

  switchTab(tab: 'login' | 'signup'): void {
    this.activeTab = tab;
    this.errorMessage = '';
    if (tab === 'signup') {
      this.successMessage = '';
    }
  }

  selectRole(role: UserRole): void {
    this.signupData.role = role;
    this.errorMessage = '';
  }

  onLogin(): void {
    this.errorMessage = '';
    this.isSubmitting = true;
    this.authService.login(this.loginData).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.authService.navigateToDashboard();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = err.error?.error || 'Login failed. Please check your credentials.';
      }
    });
  }

  onSignup(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.isSubmitting = true;
    this.authService.signup(this.signupData).subscribe({
      next: (res) => {
        this.isSubmitting = false;
        this.successMessage = res.message || 'Account created! Please log in.';
        this.loginData.username = this.signupData.username;
        this.loginData.password = '';
        this.activeTab = 'login';
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = err.error?.error || 'Sign up failed. Please verify your details.';
      }
    });
  }
}
