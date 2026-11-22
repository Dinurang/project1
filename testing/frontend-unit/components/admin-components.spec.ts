import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { AdminDashboardComponent } from '../../../frontend/app/features/admin/admin-dashboard.component';
import { ContractListComponent } from '../../../frontend/app/contract-list.component';
import { AuthService } from '../../../frontend/app/core/services/auth.service';

describe('Contract Administrator Components', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    const mockAuthService = {
      currentUser: signal({
        username: 'admin_nimal',
        fullName: 'Nimal Perera',
        role: 'CONTRACT_ADMIN',
      }),
      logout: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AdminDashboardComponent, ContractListComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('TC-FE-COMP-01: AdminDashboardComponent renders logged-in admin fullName and navigation links', () => {
    const fixture: ComponentFixture<AdminDashboardComponent> =
      TestBed.createComponent(AdminDashboardComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Nimal Perera');
    expect(compiled.textContent).toContain('Load Contract');
    expect(compiled.textContent).toContain('View Contracts');
    expect(compiled.textContent).toContain('Search Contracts');
  });

  it('TC-FE-COMP-02: ContractListComponent fetches /api/admin/contracts and displays Saved By metadata', () => {
    const fixture: ComponentFixture<ContractListComponent> =
      TestBed.createComponent(ContractListComponent);
    fixture.detectChanges();

    const req = httpMock.expectOne('http://localhost:8080/api/admin/contracts');
    expect(req.request.method).toBe('GET');

    req.flush([
      {
        id: 101,
        hotelName: 'Cinnamon Grand Colombo',
        validFrom: '2026-12-01',
        validTo: '2026-12-31',
        markup: 15,
        administrator: {
          fullName: 'Nimal Perera',
          employeeCode: 'EMP-001',
        },
        roomTypes: [
          {
            typeName: 'Deluxe Ocean View',
            price: 200,
            noOfRooms: 5,
            maxAdults: 2,
          },
        ],
      },
    ]);

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Cinnamon Grand Colombo');
    expect(compiled.textContent).toContain('Nimal Perera');
    expect(compiled.textContent).toContain('EMP-001');
    expect(compiled.textContent).toContain('Deluxe Ocean View');
  });

  it('TC-FE-COMP-03: ContractListComponent handles empty contract array without errors', () => {
    const fixture: ComponentFixture<ContractListComponent> =
      TestBed.createComponent(ContractListComponent);
    fixture.detectChanges();

    const req = httpMock.expectOne('http://localhost:8080/api/admin/contracts');
    req.flush([]);

    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });
});