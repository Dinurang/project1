import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { jwtInterceptor } from '../../../frontend/src/app/core/interceptors/jwt.interceptor';
import { AuthService } from '../../../frontend/src/app/core/services/auth.service';

describe('AuthService & JWT HTTP Interceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;
  let router: Router;

  // Helper to construct a valid unsigned test JWT payload with future expiration
  function createMockJwt(expOffsetSeconds = 3600): string {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(
      JSON.stringify({
        sub: 'admin_test',
        role: 'CONTRACT_ADMIN',
        exp: Math.floor(Date.now() / 1000) + expOffsetSeconds
      })
    );
    return `${header}.${payload}.mockSignature`;
  }

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        AuthService
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('TC-FE-SVC-01: AuthService.login() stores auth session, updates currentUser signal, and navigates to role dashboard', () => {
    const mockToken = createMockJwt(3600);

    authService.login({ username: 'admin_test', password: 'Password123' }).subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    expect(req.request.method).toBe('POST');

    req.flush({
      token: mockToken,
      username: 'admin_test',
      fullName: 'Nimal Perera',
      role: 'CONTRACT_ADMIN'
    });

    expect(authService.isLoggedIn()).toBeTrue();
    expect(authService.getRole()).toBe('CONTRACT_ADMIN');
    expect(authService.currentUser()?.fullName).toBe('Nimal Perera');
    expect(router.navigate).toHaveBeenCalledWith(['/admin/dashboard']);
  });

  it('TC-FE-SVC-02: jwtInterceptor attaches Authorization Bearer header when token is active', () => {
    const mockToken = createMockJwt(3600);
    localStorage.setItem(
      'suntravels_auth',
      JSON.stringify({
        token: mockToken,
        username: 'admin_test',
        fullName: 'Nimal Perera',
        role: 'CONTRACT_ADMIN'
      })
    );

    http.get('http://localhost:8080/api/admin/contracts').subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/admin/contracts');
    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${mockToken}`);
    req.flush([]);
  });

  it('TC-FE-SVC-03: jwtInterceptor omits Authorization header on unauthenticated requests', () => {
    http.post('http://localhost:8080/api/auth/login', { username: 'u', password: 'p' }).subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('TC-FE-SVC-04: AuthService.logout() clears storage, resets currentUser signal, and navigates to /auth', () => {
    localStorage.setItem(
      'suntravels_auth',
      JSON.stringify({
        token: createMockJwt(3600),
        username: 'admin_test',
        fullName: 'Nimal Perera',
        role: 'CONTRACT_ADMIN'
      })
    );

    authService.logout();

    expect(localStorage.getItem('suntravels_auth')).toBeNull();
    expect(authService.currentUser()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/auth']);
  });
});