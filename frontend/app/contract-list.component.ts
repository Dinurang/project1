import { Component, inject, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
@Component({
  selector: 'app-contract-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div>
      <h2>Existing Contracts</h2>
      <div *ngFor="let c of contracts" class="card">
        <h3>{{c.hotelName}} <span style="font-size:14px; color:#666;">(Valid: {{c.validFrom}} to {{c.validTo}} | Markup: {{c.markup}}%)</span></h3>
        <table>
          <tr><th>Room Type</th><th>Base Price</th><th>Total Rooms</th><th>Max Adults</th></tr>
          <tr *ngFor="let rt of c.roomTypes">
            <td>{{rt.typeName}}</td>
            <td>\${{rt.price}}</td>
            <td>{{rt.noOfRooms}}</td>
            <td>{{rt.maxAdults}}</td>
          </tr>
        </table>
      </div>
      <p *ngIf="contracts.length === 0">No contracts loaded yet.</p>
    </div>
  `
})
export class ContractListComponent implements OnInit {
  http = inject(HttpClient);
  contracts: any[] = [];
  ngOnInit() {
    this.http.get('http://localhost:8080/api/admin/contracts').subscribe((res: any) => this.contracts = res);
  }
}