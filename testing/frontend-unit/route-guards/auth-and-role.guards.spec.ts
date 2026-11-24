import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AuthService } from '../../../frontend/app/core/services/auth.service';
import { authGuard } from '../../../frontend/app/core/guards/auth.guard';
import { roleGuard } from '../../../frontend/app/core/guards/role.guard';

describe('Authentication & Role Route Guards', () => {
  // Verifies unauthenticated redirects and separation between administrator and agent routes.
  let authServiceSpy: {
    isLoggedIn: ReturnType<typeof vi.fn>;
    getRole: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  beforeEach(() => {
    authServiceSpy = {
      isLoggedIn: vi.fn(),
      getRole: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });

    router = TestBed.inject(Router);
  });

  it('TC-FE-GUARD-01: authGuard redirects unauthenticated users to /auth', () => {
    authServiceSpy.isLoggedIn.mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() => authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));
    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/auth');
  });

  it('TC-FE-GUARD-02: authGuard permits authenticated users', () => {
    authServiceSpy.isLoggedIn.mockReturnValue(true);

    const result = TestBed.runInInjectionContext(() => authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));
    expect(result).toBe(true);
  });

  it('TC-FE-GUARD-03: roleGuard blocks CALL_CENTER_AGENT from CONTRACT_ADMIN routes and redirects to /agent/dashboard', () => {
    authServiceSpy.isLoggedIn.mockReturnValue(true);
    authServiceSpy.getRole.mockReturnValue('CALL_CENTER_AGENT');

    const route = { data: { role: 'CONTRACT_ADMIN' } } as unknown as ActivatedRouteSnapshot;
    const result = TestBed.runInInjectionContext(() => roleGuard(route, {} as RouterStateSnapshot));

    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/agent/dashboard');
  });

  it('TC-FE-GUARD-04: roleGuard blocks CONTRACT_ADMIN from CALL_CENTER_AGENT routes and redirects to /admin/dashboard', () => {
    authServiceSpy.isLoggedIn.mockReturnValue(true);
    authServiceSpy.getRole.mockReturnValue('CONTRACT_ADMIN');

    const route = { data: { role: 'CALL_CENTER_AGENT' } } as unknown as ActivatedRouteSnapshot;
    const result = TestBed.runInInjectionContext(() => roleGuard(route, {} as RouterStateSnapshot));

    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/admin/dashboard');
  });

  it('TC-FE-GUARD-05: roleGuard allows access when active role matches expected role', () => {
    authServiceSpy.isLoggedIn.mockReturnValue(true);
    authServiceSpy.getRole.mockReturnValue('CONTRACT_ADMIN');

    const route = { data: { role: 'CONTRACT_ADMIN' } } as unknown as ActivatedRouteSnapshot;
    const result = TestBed.runInInjectionContext(() => roleGuard(route, {} as RouterStateSnapshot));

    expect(result).toBe(true);
  });
});