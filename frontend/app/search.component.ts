import { Component, inject } from '@angular/core';
import { FormBuilder, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
@Component({
  selector: 'app-search',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  template: `
    <div style="display: flex; gap: 30px;">
      <div class="card" style="flex: 1; height: fit-content;">
        <h2>Search Criteria</h2>
        <form [formGroup]="searchForm" (ngSubmit)="onSearch()">
          <label>Check-in Date:</label>
          <input type="date" formControlName="checkInDate" required style="width: 100%;">

          <label>No of Nights:</label>
          <input type="number" formControlName="noOfNights" required style="width: 100%;">

          <div formArrayName="roomRequests" style="margin-top: 20px;">
            <h3>Rooms Required</h3>
            <div *ngFor="let room of roomRequests.controls; let i=index" [formGroupName]="i" style="margin-bottom: 10px; display:flex; gap:10px; align-items:flex-end;">
              <div style="flex:1;">
                <label>Adults in Room {{i+1}}:</label>
                <input type="number" formControlName="adults" required style="width: 100%;">
              </div>
              <button type="button" class="btn-danger" (click)="removeRoom(i)" *ngIf="roomRequests.length > 1">Remove</button>
            </div>
          </div>
          <button type="button" class="btn" (click)="addRoom()">+ Add Room</button>
          <hr style="margin: 20px 0; border: 0; border-top: 1px solid #ddd;">
          <button type="submit" class="btn" style="width: 100%; font-size: 16px;" [disabled]="searchForm.invalid">Search Availabilities</button>
        </form>
      </div>

      <div class="card" style="flex: 2;" *ngIf="searched">
        <h2>Search Results</h2>
        <table *ngIf="results.length > 0">
          <tr><th>Hotel Name</th><th>Room Type</th><th>Total Price (Marked Up)</th><th>Status</th></tr>
          <tr *ngFor="let res of results">
            <td>{{res.hotelName}}</td>
            <td>{{res.roomType}}</td>
            <td style="font-weight:bold;">\${{res.price | number:'1.2-2'}}</td>
            <td [ngStyle]="{'color': res.status === 'Available' ? 'green' : 'red', 'font-weight': 'bold'}">{{res.status}}</td>
          </tr>
        </table>
        <p *ngIf="results.length === 0">No hotels matched the criteria in the valid contract periods.</p>
      </div>
    </div>
  `
})
export class SearchComponent {
  fb = inject(FormBuilder);
  http = inject(HttpClient);

  searchForm = this.fb.group({
    checkInDate: ['', Validators.required],
    noOfNights: [1, [Validators.required, Validators.min(1)]],
    roomRequests: this.fb.array([this.createRoom()])
  });

  results: any[] = [];
  searched = false;

  get roomRequests() { return this.searchForm.get('roomRequests') as FormArray; }
  createRoom() { return this.fb.group({ adults: [1, [Validators.required, Validators.min(1)]] }); }
  addRoom() { this.roomRequests.push(this.createRoom()); }
  removeRoom(index: number) { this.roomRequests.removeAt(index); }

  onSearch() {
    // The backend calculates contract validity, capacity, and the marked-up total.
    this.http.post('http://localhost:8080/api/agent/search', this.searchForm.value).subscribe({
      next: (res: any) => { this.results = res; this.searched = true; },
      error: () => alert('Error fetching results')
    });
  }
}