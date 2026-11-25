import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AppComponent } from '../../../frontend/app/app.component';
import { ContractFormComponent } from '../../../frontend/app/contract-form.component';
import { SearchComponent } from '../../../frontend/app/search.component';
import { SearchContractComponent } from '../../../frontend/app/search-contract/search-contract.component';
import { AgentDashboardComponent } from '../../../frontend/app/features/agent/agent-dashboard.component';
import { AuthComponent } from '../../../frontend/app/features/auth/auth.component';
import { AuthService } from '../../../frontend/app/core/services/auth.service';

describe('Application Page Components', () => {
  let httpMock: HttpTestingController;
  let authServiceMock: {
    currentUser: ReturnType<typeof signal<{ username: string; fullName: string; role: string } | null>>;
    isLoggedIn: ReturnType<typeof vi.fn>;
    getRole: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
    login: ReturnType<typeof vi.fn>;
    signup: ReturnType<typeof vi.fn>;
    navigateToDashboard: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceMock = {
      currentUser: signal({
        username: 'agent_kasun',
        fullName: 'Kasun Fernando',
        role: 'CALL_CENTER_AGENT',
      }),
      isLoggedIn: vi.fn(() => true),
      getRole: vi.fn(() => 'CALL_CENTER_AGENT'),
      logout: vi.fn(),
      login: vi.fn(() => of({
        token: 'test-token',
        username: 'agent_kasun',
        fullName: 'Kasun Fernando',
        role: 'CALL_CENTER_AGENT' as const,
      })),
      signup: vi.fn(() => of({ message: 'Registration successful.' })),
      navigateToDashboard: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [
        AppComponent,
        AgentDashboardComponent,
        AuthComponent,
        ContractFormComponent,
        SearchComponent,
        SearchContractComponent,
      ],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('TC-FE-PAGE-01: AppComponent hides navigation when logged out and logs out an authenticated user', () => {
    authServiceMock.isLoggedIn.mockReturnValue(false);
    const fixture: ComponentFixture<AppComponent> = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('nav')).toBeNull();

    authServiceMock.isLoggedIn.mockReturnValue(true);
    authServiceMock.getRole.mockReturnValue('CONTRACT_ADMIN');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Sun Travels');
    expect(compiled.textContent).toContain('Contract Administrator');
    (compiled.querySelector('.logout-btn') as HTMLButtonElement).click();
    expect(authServiceMock.logout).toHaveBeenCalledOnce();
  });

  it('TC-FE-PAGE-02: AgentDashboardComponent displays the agent name and room-search link', () => {
    const fixture: ComponentFixture<AgentDashboardComponent> =
      TestBed.createComponent(AgentDashboardComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Call Center Agent Dashboard');
    expect(compiled.textContent).toContain('Kasun Fernando');
    expect(compiled.querySelector('a[routerLink="/agent/search"]')?.textContent).toContain('Search Rooms');
  });

  it('TC-FE-PAGE-03: AuthComponent switches signup roles and submits administrator registration', () => {
    const fixture: ComponentFixture<AuthComponent> = TestBed.createComponent(AuthComponent);
    fixture.detectChanges();

    const signupButton = [...fixture.nativeElement.querySelectorAll('button')]
      .find((button: HTMLButtonElement) => button.textContent?.trim() === 'Sign Up');
    signupButton?.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#agentCode')).not.toBeNull();

    fixture.componentInstance.selectRole('CONTRACT_ADMIN');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#employeeCode')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#agentCode')).toBeNull();

    fixture.componentInstance.signupData = {
      username: 'new_admin',
      email: 'new_admin@suntravels.com',
      password: 'Password123!',
      role: 'CONTRACT_ADMIN',
      fullName: 'New Administrator',
      employeeCode: 'EMP-501',
      contactNo: '0771234567',
    };
    fixture.componentInstance.onSignup();
    fixture.detectChanges();

    expect(authServiceMock.signup).toHaveBeenCalledWith(fixture.componentInstance.signupData);
    expect(fixture.componentInstance.activeTab).toBe('login');
    expect(fixture.componentInstance.loginData.username).toBe('new_admin');
    expect(fixture.nativeElement.textContent).toContain('Registration successful.');
  });

  it('TC-FE-PAGE-04: AuthComponent shows login failures and clears the submitting state', () => {
    authServiceMock.login.mockReturnValue(
      throwError(() => ({ error: { error: 'Invalid credentials.' } }))
    );
    const fixture: ComponentFixture<AuthComponent> = TestBed.createComponent(AuthComponent);
    fixture.detectChanges();

    fixture.componentInstance.loginData = { username: 'unknown', password: 'bad-password' };
    fixture.componentInstance.onLogin();
    fixture.detectChanges();

    expect(fixture.componentInstance.isSubmitting).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Invalid credentials.');
    expect(authServiceMock.navigateToDashboard).not.toHaveBeenCalled();
  });

  it('TC-FE-PAGE-05: AuthComponent navigates after successful login', () => {
    const fixture: ComponentFixture<AuthComponent> = TestBed.createComponent(AuthComponent);
    fixture.detectChanges();

    fixture.componentInstance.loginData = { username: 'agent_kasun', password: 'Password123!' };
    fixture.componentInstance.onLogin();

    expect(fixture.componentInstance.isSubmitting).toBe(false);
    expect(authServiceMock.navigateToDashboard).toHaveBeenCalledOnce();
  });

  it('TC-FE-PAGE-06: ContractFormComponent submits the contract with room types and resets on success', () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => undefined);
    const fixture: ComponentFixture<ContractFormComponent> =
      TestBed.createComponent(ContractFormComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.contractForm.invalid).toBe(true);
    fixture.componentInstance.contractForm.patchValue({
      hotelName: 'Jetwing Lighthouse',
      validFrom: '2027-01-01',
      validTo: '2027-01-31',
      markup: 15,
    });
    fixture.componentInstance.roomTypes.at(0).patchValue({
      typeName: 'Ocean View',
      price: 180,
      noOfRooms: 6,
      maxAdults: 2,
    });
    fixture.componentInstance.addRoomType();
    fixture.componentInstance.roomTypes.at(1).patchValue({
      typeName: 'Garden Suite',
      price: 250,
      noOfRooms: 3,
      maxAdults: 4,
    });
    fixture.detectChanges();
    expect(fixture.componentInstance.contractForm.valid).toBe(true);

    const saveButton = [...fixture.nativeElement.querySelectorAll('button')]
      .find((button: HTMLButtonElement) => button.textContent?.includes('Save Contract'));
    saveButton?.click();

    const request = httpMock.expectOne('http://localhost:8080/api/admin/contracts');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.hotelName).toBe('Jetwing Lighthouse');
    expect(request.request.body.roomTypes).toHaveLength(2);
    request.flush({ id: 501 });

    fixture.detectChanges();
    expect(alertSpy).toHaveBeenCalledWith('Contract saved successfully!');
    expect(fixture.componentInstance.contractForm.controls.hotelName.value).toBeNull();
    expect(fixture.componentInstance.contractForm.controls.markup.value).toBe(15);
    expect(fixture.componentInstance.roomTypes.length).toBe(1);
  });

  it('TC-FE-PAGE-07: ContractFormComponent reports a failed save', () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => undefined);
    const fixture: ComponentFixture<ContractFormComponent> =
      TestBed.createComponent(ContractFormComponent);
    fixture.detectChanges();

    fixture.componentInstance.contractForm.patchValue({
      hotelName: 'Jetwing Lighthouse',
      validFrom: '2027-01-01',
      validTo: '2027-01-31',
    });
    fixture.componentInstance.roomTypes.at(0).patchValue({
      typeName: 'Ocean View',
      price: 180,
      noOfRooms: 6,
      maxAdults: 2,
    });
    fixture.componentInstance.onSubmit();

    httpMock.expectOne('http://localhost:8080/api/admin/contracts')
      .flush({ message: 'Failure' }, { status: 500, statusText: 'Server Error' });
    expect(alertSpy).toHaveBeenCalledWith('Error saving contract.');
  });

  it('TC-FE-PAGE-08: SearchComponent posts criteria and renders available and unavailable results', () => {
    const fixture: ComponentFixture<SearchComponent> = TestBed.createComponent(SearchComponent);
    fixture.detectChanges();
    fixture.componentInstance.searchForm.patchValue({
      checkInDate: '2027-01-10',
      noOfNights: 2,
    });
    fixture.componentInstance.addRoom();
    fixture.componentInstance.roomRequests.at(1).patchValue({ adults: 2 });
    fixture.detectChanges();

    const searchButton = [...fixture.nativeElement.querySelectorAll('button')]
      .find((button: HTMLButtonElement) => button.textContent?.includes('Search Availabilities'));
    expect(searchButton?.disabled).toBe(false);
    searchButton?.click();

    const request = httpMock.expectOne('http://localhost:8080/api/agent/search');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.roomRequests).toHaveLength(2);
    request.flush([
      { hotelName: 'Jetwing Lighthouse', roomType: 'Ocean View', price: 720, status: 'Available' },
      { hotelName: 'Cinnamon Grand', roomType: 'Deluxe', price: null, status: 'Unavailable' },
    ]);

    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Jetwing Lighthouse');
    expect(fixture.nativeElement.textContent).toContain('$720.00');
    expect(fixture.nativeElement.textContent).toContain('Unavailable');
  });

  it('TC-FE-PAGE-09: SearchComponent displays the empty state when no results match', () => {
    const fixture: ComponentFixture<SearchComponent> = TestBed.createComponent(SearchComponent);
    fixture.detectChanges();
    fixture.componentInstance.searchForm.patchValue({ checkInDate: '2027-01-10' });
    fixture.componentInstance.onSearch();
    httpMock.expectOne('http://localhost:8080/api/agent/search').flush([]);

    fixture.detectChanges();
    expect(fixture.nativeElement.textContent)
      .toContain('No hotels matched the criteria in the valid contract periods.');
  });

  it('TC-FE-PAGE-10: SearchComponent reports API errors', () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => undefined);
    const fixture: ComponentFixture<SearchComponent> = TestBed.createComponent(SearchComponent);
    fixture.detectChanges();
    fixture.componentInstance.searchForm.patchValue({ checkInDate: '2027-01-10' });
    fixture.componentInstance.onSearch();
    httpMock.expectOne('http://localhost:8080/api/agent/search')
      .flush({ message: 'Failure' }, { status: 500, statusText: 'Server Error' });

    expect(alertSpy).toHaveBeenCalledWith('Error fetching results');
  });

  it('TC-FE-PAGE-11: SearchContractComponent loads contracts, filters results, and resets filters', () => {
    const fixture: ComponentFixture<SearchContractComponent> =
      TestBed.createComponent(SearchContractComponent);
    fixture.detectChanges();

    const initialRequest = httpMock.expectOne(request =>
      request.url === 'http://localhost:8080/api/admin/contracts/search' &&
      request.params.keys().length === 0
    );
    expect(initialRequest.request.method).toBe('GET');
    initialRequest.flush([]);

    fixture.componentInstance.hotelName = '  Jetwing  ';
    fixture.componentInstance.activeDate = '2027-01-15';
    fixture.componentInstance.onSearch();
    const filteredRequest = httpMock.expectOne(request =>
      request.url === 'http://localhost:8080/api/admin/contracts/search' &&
      request.params.get('hotelName') === 'Jetwing' &&
      request.params.get('activeDate') === '2027-01-15'
    );
    filteredRequest.flush([{
      id: 501,
      hotelName: 'Jetwing Lighthouse',
      validFrom: '2027-01-01',
      validTo: '2027-01-31',
      markup: 15,
      roomTypes: [{ typeName: 'Ocean View', price: 180, noOfRooms: 6, maxAdults: 2 }],
    }]);

    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Jetwing Lighthouse');
    expect(fixture.nativeElement.textContent).toContain('Ocean View');
    expect(fixture.nativeElement.textContent).toContain('$180.00');

    fixture.componentInstance.onReset();
    const resetRequest = httpMock.expectOne(request =>
      request.url === 'http://localhost:8080/api/admin/contracts/search' &&
      request.params.keys().length === 0
    );
    resetRequest.flush([]);
    expect(fixture.componentInstance.hotelName).toBe('');
    expect(fixture.componentInstance.activeDate).toBe('');
  });

  it('TC-FE-PAGE-12: SearchContractComponent clears results and shows a helpful message on API failure', () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const fixture: ComponentFixture<SearchContractComponent> =
      TestBed.createComponent(SearchContractComponent);
    fixture.detectChanges();

    httpMock.expectOne('http://localhost:8080/api/admin/contracts/search')
      .flush({ message: 'Failure' }, { status: 500, statusText: 'Server Error' });

    fixture.detectChanges();
    expect(consoleErrorSpy).toHaveBeenCalled();
    expect(fixture.componentInstance.contracts).toEqual([]);
    expect(fixture.nativeElement.textContent)
      .toContain('Failed to search contracts. Ensure the backend is running.');
  });
});
