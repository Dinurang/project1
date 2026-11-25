import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

interface RoomType {
  typeName: string;
  price: number | null;
  noOfRooms: number | null;
  maxAdults: number | null;
}

interface ContractRecord {
  id: number;
  hotelName: string;
  validFrom: string;
  validTo: string;
  markup: number;
  roomTypes: RoomType[];
}

@Component({
  selector: 'app-contract-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section>
      <h2>Edit Contracts</h2>
      <p *ngIf="successMessage" class="success-message" role="status">{{ successMessage }}</p>
      <p *ngIf="errorMessage" class="error-message" role="alert">{{ errorMessage }}</p>

      <p *ngIf="contracts.length === 0">No contracts loaded yet.</p>
      <article *ngFor="let contract of contracts" class="contract-card">
        <div class="contract-heading">
          <div>
            <h3>{{ contract.hotelName }}</h3>
            <p>Valid: {{ contract.validFrom }} to {{ contract.validTo }} | Markup: {{ contract.markup }}%</p>
          </div>
          <div class="actions">
            <button type="button" class="btn" (click)="startEditing(contract)">Edit</button>
            <button type="button" class="btn-danger" (click)="deleteContract(contract)">Delete</button>
          </div>
        </div>

        <table>
          <thead>
            <tr><th>Room Type</th><th>Base Price</th><th>Total Rooms</th><th>Max Adults</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let roomType of contract.roomTypes">
              <td>{{ roomType.typeName }}</td>
              <td>{{ roomType.price }}</td>
              <td>{{ roomType.noOfRooms }}</td>
              <td>{{ roomType.maxAdults }}</td>
            </tr>
          </tbody>
        </table>

        <form *ngIf="selectedContractId === contract.id"
              [formGroup]="contractForm" (ngSubmit)="saveChanges(contract.id)" class="edit-form">
          <h4>Update contract details</h4>
          <label>Hotel Name
            <input formControlName="hotelName" required>
          </label>
          <div class="contract-fields">
            <label>Valid From
              <input type="date" formControlName="validFrom" required>
            </label>
            <label>Valid To
              <input type="date" formControlName="validTo" required>
            </label>
            <label>Markup (%)
              <input type="number" formControlName="markup" min="0" required>
            </label>
          </div>
          <div formArrayName="roomTypes">
            <h4>Room Types</h4>
            <div *ngFor="let roomType of roomTypes.controls; let i = index"
                 [formGroupName]="i" class="room-fields">
              <input placeholder="Room type" formControlName="typeName" required>
              <input type="number" placeholder="Base price" formControlName="price" min="0" required>
              <input type="number" placeholder="Total rooms" formControlName="noOfRooms" min="0" required>
              <input type="number" placeholder="Max adults" formControlName="maxAdults" min="1" required>
              <button *ngIf="roomTypes.length > 1" type="button" class="btn-danger"
                      (click)="removeRoomType(i)" aria-label="Remove room type">Remove</button>
            </div>
            <button type="button" class="btn" (click)="addRoomType()">+ Add Room Type</button>
          </div>
          <div class="actions form-actions">
            <button type="submit" class="btn" [disabled]="contractForm.invalid">Save Changes</button>
            <button type="button" class="btn-secondary" (click)="cancelEditing()">Cancel</button>
          </div>
        </form>
      </article>
    </section>
  `,
  styles: [`
    .contract-card { background: white; border-radius: 8px; padding: 18px; margin: 16px 0; box-shadow: 0 2px 4px rgba(0,0,0,.08); }
    .contract-heading, .actions { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
    .contract-heading h3, .contract-heading p { margin: 0 0 6px; }
    .edit-form { border-top: 1px solid #ddd; margin-top: 18px; padding-top: 12px; }
    label { display: block; margin: 8px 0; font-weight: 600; }
    input { box-sizing: border-box; width: 100%; padding: 8px; margin-top: 4px; }
    .contract-fields, .room-fields { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
    .room-fields { grid-template-columns: repeat(4, minmax(0, 1fr)) auto; margin-bottom: 10px; }
    .form-actions { justify-content: flex-start; margin-top: 16px; }
    .success-message { color: #176b32; }
    .error-message { color: #a32121; }
    table { border-collapse: collapse; width: 100%; margin-top: 12px; }
    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
    @media (max-width: 700px) {
      .contract-heading { align-items: flex-start; flex-direction: column; }
      .contract-fields, .room-fields { grid-template-columns: 1fr; }
    }
  `]
})
export class ContractManagementComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);
  private readonly apiUrl = 'http://localhost:8080/api/admin/contracts';

  contracts: ContractRecord[] = [];
  selectedContractId: number | null = null;
  successMessage = '';
  errorMessage = '';

  readonly contractForm = this.fb.group({
    hotelName: ['', Validators.required],
    validFrom: ['', Validators.required],
    validTo: ['', Validators.required],
    markup: [15, [Validators.required, Validators.min(0)]],
    roomTypes: this.fb.array([])
  });

  get roomTypes(): FormArray {
    return this.contractForm.controls.roomTypes;
  }

  ngOnInit(): void {
    this.loadContracts();
  }

  startEditing(contract: ContractRecord): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.selectedContractId = contract.id;
    this.contractForm.patchValue({
      hotelName: contract.hotelName,
      validFrom: contract.validFrom,
      validTo: contract.validTo,
      markup: contract.markup
    });
    this.roomTypes.clear();
    contract.roomTypes.forEach(roomType => this.roomTypes.push(this.createRoomType(roomType)));
    if (this.roomTypes.length === 0) {
      this.addRoomType();
    }
  }

  addRoomType(): void {
    this.roomTypes.push(this.createRoomType());
  }

  removeRoomType(index: number): void {
    if (this.roomTypes.length > 1) {
      this.roomTypes.removeAt(index);
    }
  }

  saveChanges(contractId: number): void {
    if (this.contractForm.invalid) {
      this.contractForm.markAllAsTouched();
      return;
    }

    this.http.put<ContractRecord>(`${this.apiUrl}/${contractId}`, this.contractForm.getRawValue()).subscribe({
      next: updatedContract => {
        const index = this.contracts.findIndex(contract => contract.id === contractId);
        if (index >= 0) {
          this.contracts[index] = updatedContract;
        }
        this.selectedContractId = null;
        this.successMessage = 'Contract updated successfully.';
        this.errorMessage = '';
      },
      error: () => {
        this.errorMessage = 'Unable to update the contract. Please try again.';
        this.successMessage = '';
      }
    });
  }

  deleteContract(contract: ContractRecord): void {
    if (!window.confirm(`Delete the contract for ${contract.hotelName}? This cannot be undone.`)) {
      return;
    }

    this.http.delete<void>(`${this.apiUrl}/${contract.id}`).subscribe({
      next: () => {
        this.contracts = this.contracts.filter(item => item.id !== contract.id);
        if (this.selectedContractId === contract.id) {
          this.selectedContractId = null;
        }
        this.successMessage = 'Contract deleted successfully.';
        this.errorMessage = '';
      },
      error: () => {
        this.errorMessage = 'Unable to delete the contract. Please try again.';
        this.successMessage = '';
      }
    });
  }

  private loadContracts(): void {
    this.http.get<ContractRecord[]>(this.apiUrl).subscribe({
      next: contracts => {
        this.contracts = contracts;
        this.errorMessage = '';
      },
      error: () => {
        this.errorMessage = 'Unable to load contracts. Please try again.';
      }
    });
  }

  private createRoomType(roomType?: RoomType) {
    return this.fb.group({
      typeName: [roomType?.typeName ?? '', Validators.required],
      price: [roomType?.price ?? null, [Validators.required, Validators.min(0)]],
      noOfRooms: [roomType?.noOfRooms ?? null, [Validators.required, Validators.min(0)]],
      maxAdults: [roomType?.maxAdults ?? null, [Validators.required, Validators.min(1)]]
    });
  }
}
