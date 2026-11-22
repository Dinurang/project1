import { Component, inject } from '@angular/core';
import { FormBuilder, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
@Component({
  selector: 'app-contract-form',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  template: `
    <div class="card">
      <h2>Load New Contract</h2>
      <form [formGroup]="contractForm" (ngSubmit)="onSubmit()">
        <label>Hotel Name:</label>
        <input formControlName="hotelName" required style="width: 100%;">
        
        <div style="display: flex; gap: 20px;">
            <div style="flex: 1;">
                <label>Valid From:</label>
                <input type="date" formControlName="validFrom" required style="width: 100%;">
            </div>
            <div style="flex: 1;">
                <label>Valid To:</label>
                <input type="date" formControlName="validTo" required style="width: 100%;">
            </div>
            <div style="flex: 1;">
                <label>Markup (%):</label>
                <input type="number" formControlName="markup" required style="width: 100%;">
            </div>
        </div>

        <div formArrayName="roomTypes" style="margin-top: 20px;">
          <h3>Room Types</h3>
          <div *ngFor="let rt of roomTypes.controls; let i=index" [formGroupName]="i" style="display:flex; gap:10px; margin-bottom:10px;">
            <input placeholder="Type (e.g. Standard)" formControlName="typeName" required style="flex:2;">
            <input type="number" placeholder="Price ($)" formControlName="price" required style="flex:1;">
            <input type="number" placeholder="Total Rooms" formControlName="noOfRooms" required style="flex:1;">
            <input type="number" placeholder="Max Adults" formControlName="maxAdults" required style="flex:1;">
            <button type="button" class="btn-danger" (click)="removeRoomType(i)">X</button>
          </div>
          <button type="button" class="btn" (click)="addRoomType()">+ Add Room Type</button>
        </div>

        <button type="submit" class="btn" style="margin-top: 20px; width: 100%;" [disabled]="contractForm.invalid">Save Contract</button>
      </form>
    </div>
  `
})
export class ContractFormComponent {
  fb = inject(FormBuilder);
  http = inject(HttpClient);

  // The default markup follows the assignment's 15 percent hotel markup.
  contractForm = this.fb.group({
    hotelName: ['', Validators.required],
    validFrom: ['', Validators.required],
    validTo: ['', Validators.required],
    markup: [15, [Validators.required, Validators.min(0)]],
    roomTypes: this.fb.array([this.createRoomType()])
  });

  get roomTypes() { return this.contractForm.get('roomTypes') as FormArray; }
  createRoomType() {
    return this.fb.group({
      typeName: ['', Validators.required],
      price: [null, Validators.required],
      noOfRooms: [null, Validators.required],
      maxAdults: [null, Validators.required]
    });
  }
  addRoomType() { this.roomTypes.push(this.createRoomType()); }
  removeRoomType(index: number) { this.roomTypes.removeAt(index); }

  onSubmit() {
    // The contract includes nested roomTypes, which JPA saves as one contract/roomType group.
    this.http.post('http://localhost:8080/api/admin/contracts', this.contractForm.value).subscribe({
      next: () => { alert('Contract saved successfully!'); this.contractForm.reset({markup: 15}); this.roomTypes.clear(); this.addRoomType(); },
      error: () => alert('Error saving contract.')
    });
  }
}