import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, SignupRequest, UserRole } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // All authentication state is stored in localStorage so it survives page refreshes.
  private readonly apiUrl = 'http://localhost:8080/api/auth';
  private readonly storageKey = 'suntravels_auth';

  currentUser = signal<AuthResponse | null>(this.loadStoredUser());

  constructor(private http: HttpClient, private router: Router) {}

  signup(payload: SignupRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/signup`, payload);
  }

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, payload).pipe(
      tap((res) => {
        // Save the token and role together as the current browser session.
        localStorage.setItem(this.storageKey, JSON.stringify(res));
        this.currentUser.set(res);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.storageKey);
    this.currentUser.set(null);
    this.router.navigate(['/auth']);
  }

  getToken(): string | null {
    return this.currentUser()?.token ?? null;
  }

  getRole(): UserRole | null {
    return this.currentUser()?.role ?? null;
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) return false;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  navigateToDashboard(): void {
    const role = this.getRole();
    if (role === 'CONTRACT_ADMIN') {
      this.router.navigate(['/admin/dashboard']);
    } else if (role === 'CALL_CENTER_AGENT') {
      this.router.navigate(['/agent/dashboard']);
    } else {
      this.router.navigate(['/auth']);
    }
  }

  private loadStoredUser(): AuthResponse | null {
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) return null;
    try {
      const parsed: AuthResponse = JSON.parse(raw);
      const payload = JSON.parse(atob(parsed.token.split('.')[1]));
      if (payload.exp * 1000 > Date.now()) {
        return parsed;
      }
      localStorage.removeItem(this.storageKey);
      return null;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }
}
