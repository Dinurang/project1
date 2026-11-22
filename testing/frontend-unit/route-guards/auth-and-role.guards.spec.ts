import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { AuthService } from '../../../frontend/src/app/core/services/auth.service';
import { authGuard, roleGuard } from '../../../frontend/src/app/core/guards/auth.guard';

describe('Authentication & Role Route Guards', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['isLoggedIn', 'getRole']);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });

    router = TestBed.inject(Router);
  });

  it('TC-FE-GUARD-01: authGuard redirects unauthenticated users to /auth', () => {
    authServiceSpy.isLoggedIn.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/auth');
  });

  it('TC-FE-GUARD-02: authGuard permits authenticated users', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    expect(result).toBeTrue();
  });

  it('TC-FE-GUARD-03: roleGuard blocks CALL_CENTER_AGENT from CONTRACT_ADMIN routes and redirects to /agent/dashboard', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.getRole.and.returnValue('CALL_CENTER_AGENT');

    const guard = roleGuard('CONTRACT_ADMIN');
    const result = TestBed.runInInjectionContext(() => guard({} as any, {} as any));

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/agent/dashboard');
  });

  it('TC-FE-GUARD-04: roleGuard blocks CONTRACT_ADMIN from CALL_CENTER_AGENT routes and redirects to /admin/dashboard', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.getRole.and.returnValue('CONTRACT_ADMIN');

    const guard = roleGuard('CALL_CENTER_AGENT');
    const result = TestBed.runInInjectionContext(() => guard({} as any, {} as any));

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/admin/dashboard');
  });

  it('TC-FE-GUARD-05: roleGuard allows access when active role matches expected role', () => {
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.getRole.and.returnValue('CONTRACT_ADMIN');

    const guard = roleGuard('CONTRACT_ADMIN');
    const result = TestBed.runInInjectionContext(() => guard({} as any, {} as any));

    expect(result).toBeTrue();
  });
});