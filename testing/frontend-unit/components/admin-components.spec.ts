import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { AdminDashboardComponent } from '../../../frontend/app/features/admin/admin-dashboard.component';
import { ContractManagementComponent } from '../../../frontend/app/features/admin/contract-management.component';
import { ContractListComponent } from '../../../frontend/app/contract-list.component';
import { AuthService } from '../../../frontend/app/core/services/auth.service';

describe('Contract Administrator Components', () => {
  // Verifies rendered administrator data and contract HTTP states.
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
      imports: [AdminDashboardComponent, ContractListComponent, ContractManagementComponent],
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
    vi.restoreAllMocks();
  });

  it('TC-FE-COMP-01: AdminDashboardComponent renders logged-in admin fullName and navigation links', () => {
    const fixture: ComponentFixture<AdminDashboardComponent> =
      TestBed.createComponent(AdminDashboardComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Nimal Perera');
    expect(compiled.textContent).toContain('Load Contract');
    expect(compiled.textContent).toContain('View Contracts');
    expect(compiled.textContent).toContain('Edit Contracts');
    expect(compiled.textContent).toContain('Search Contracts');
    expect(compiled.querySelector('a[routerLink="/admin/edit-contracts"]')).not.toBeNull();
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

  it('TC-FE-COMP-04: Edit Contracts loads contracts and saves edited contract and room details', () => {
    const fixture: ComponentFixture<ContractManagementComponent> =
      TestBed.createComponent(ContractManagementComponent);
    fixture.detectChanges();

    httpMock.expectOne('http://localhost:8080/api/admin/contracts').flush([{
      id: 101,
      hotelName: 'Cinnamon Grand Colombo',
      validFrom: '2026-12-01',
      validTo: '2026-12-31',
      markup: 15,
      roomTypes: [{ typeName: 'Deluxe', price: 200, noOfRooms: 5, maxAdults: 2 }],
    }]);
    fixture.detectChanges();

    const editButton = [...fixture.nativeElement.querySelectorAll('button')]
      .find((button: HTMLButtonElement) => button.textContent?.trim() === 'Edit');
    expect(editButton).toBeDefined();
    editButton?.click();
    fixture.detectChanges();

    fixture.componentInstance.addRoomType();
    expect(fixture.componentInstance.roomTypes.length).toBe(2);
    fixture.componentInstance.removeRoomType(1);
    fixture.componentInstance.removeRoomType(0);
    expect(fixture.componentInstance.roomTypes.length).toBe(1);

    fixture.componentInstance.contractForm.patchValue({
      hotelName: 'Updated Cinnamon Grand',
      validFrom: '2027-01-01',
      validTo: '2027-01-31',
      markup: 20,
    });
    fixture.componentInstance.roomTypes.at(0).patchValue({ typeName: 'Updated Deluxe' });
    fixture.detectChanges();

    const saveButton = [...fixture.nativeElement.querySelectorAll('button')]
      .find((button: HTMLButtonElement) => button.textContent?.trim() === 'Save Changes');
    saveButton?.click();

    const request = httpMock.expectOne('http://localhost:8080/api/admin/contracts/101');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body.hotelName).toBe('Updated Cinnamon Grand');
    expect(request.request.body.roomTypes[0].typeName).toBe('Updated Deluxe');
    request.flush({
      id: 101,
      hotelName: 'Updated Cinnamon Grand',
      validFrom: '2027-01-01',
      validTo: '2027-01-31',
      markup: 20,
      roomTypes: [{ typeName: 'Updated Deluxe', price: 200, noOfRooms: 5, maxAdults: 2 }],
    });

    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Contract updated successfully.');
    expect(fixture.nativeElement.textContent).toContain('Updated Cinnamon Grand');
  });

  it('TC-FE-COMP-05: Edit Contracts asks for delete confirmation and removes the contract on success', () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);
    const fixture: ComponentFixture<ContractManagementComponent> =
      TestBed.createComponent(ContractManagementComponent);
    fixture.detectChanges();

    httpMock.expectOne('http://localhost:8080/api/admin/contracts').flush([{
      id: 101,
      hotelName: 'Cinnamon Grand Colombo',
      validFrom: '2026-12-01',
      validTo: '2026-12-31',
      markup: 15,
      roomTypes: [{ typeName: 'Deluxe', price: 200, noOfRooms: 5, maxAdults: 2 }],
    }]);
    fixture.detectChanges();

    const deleteButton = [...fixture.nativeElement.querySelectorAll('button')]
      .find((button: HTMLButtonElement) => button.textContent?.trim() === 'Delete');
    deleteButton?.click();
    expect(confirmSpy).toHaveBeenCalledOnce();
    httpMock.expectNone('http://localhost:8080/api/admin/contracts/101');

    confirmSpy.mockReturnValue(true);
    deleteButton?.click();
    const request = httpMock.expectOne('http://localhost:8080/api/admin/contracts/101');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);

    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Contract deleted successfully.');
    expect(fixture.nativeElement.textContent).toContain('No contracts loaded yet.');
  });

  it('TC-FE-COMP-06: Edit Contracts shows a clear message when an update fails', () => {
    const fixture: ComponentFixture<ContractManagementComponent> =
      TestBed.createComponent(ContractManagementComponent);
    fixture.detectChanges();

    httpMock.expectOne('http://localhost:8080/api/admin/contracts').flush([{
      id: 101,
      hotelName: 'Cinnamon Grand Colombo',
      validFrom: '2026-12-01',
      validTo: '2026-12-31',
      markup: 15,
      roomTypes: [{ typeName: 'Deluxe', price: 200, noOfRooms: 5, maxAdults: 2 }],
    }]);

    fixture.componentInstance.startEditing(fixture.componentInstance.contracts[0]);
    fixture.detectChanges();
    fixture.componentInstance.contractForm.patchValue({ hotelName: 'Updated Hotel' });
    fixture.detectChanges();

    const saveButton = [...fixture.nativeElement.querySelectorAll('button')]
      .find((button: HTMLButtonElement) => button.textContent?.trim() === 'Save Changes');
    saveButton?.click();
    httpMock.expectOne('http://localhost:8080/api/admin/contracts/101')
      .flush({ message: 'Failure' }, { status: 500, statusText: 'Server Error' });

    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Unable to update the contract.');
  });

  it('TC-FE-COMP-07: Edit Contracts shows a clear message when a confirmed deletion fails', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const fixture: ComponentFixture<ContractManagementComponent> =
      TestBed.createComponent(ContractManagementComponent);
    fixture.detectChanges();

    httpMock.expectOne('http://localhost:8080/api/admin/contracts').flush([{
      id: 101,
      hotelName: 'Cinnamon Grand Colombo',
      validFrom: '2026-12-01',
      validTo: '2026-12-31',
      markup: 15,
      roomTypes: [{ typeName: 'Deluxe', price: 200, noOfRooms: 5, maxAdults: 2 }],
    }]);
    fixture.detectChanges();

    const deleteButton = [...fixture.nativeElement.querySelectorAll('button')]
      .find((button: HTMLButtonElement) => button.textContent?.trim() === 'Delete');
    deleteButton?.click();
    httpMock.expectOne('http://localhost:8080/api/admin/contracts/101')
      .flush({ message: 'Failure' }, { status: 500, statusText: 'Server Error' });

    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Unable to delete the contract.');
    expect(fixture.nativeElement.textContent).toContain('Cinnamon Grand Colombo');
  });
});