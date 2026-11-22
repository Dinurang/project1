import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';

interface RoomType {
  id?: number;
  typeName: string;
  price: number;
  noOfRooms: number;
  maxAdults: number;
}

interface Contract {
  id: number;
  hotelName: string;
  validFrom: string;
  validTo: string;
  markup: number;
  roomTypes: RoomType[];
}

@Component({
  selector: 'app-search-contract',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './search-contract.component.html',
  styleUrls: ['./search-contract.component.css']
})
export class SearchContractComponent implements OnInit {
  hotelName: string = '';
  activeDate: string = '';
  contracts: Contract[] = [];
  errorMessage: string = '';

  private apiUrl = 'http://localhost:8080/api/admin/contracts/search';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.onSearch();
  }

  onSearch(): void {
    this.errorMessage = '';
    let params = new HttpParams();

    if (this.hotelName.trim()) {
      params = params.set('hotelName', this.hotelName.trim());
    }
    if (this.activeDate) {
      params = params.set('activeDate', this.activeDate);
    }

    this.http.get<Contract[]>(this.apiUrl, { params }).subscribe({
      next: (data) => {
        this.contracts = data;
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Failed to search contracts. Ensure the backend is running.';
        this.contracts = [];
      }
    });
  }

  onReset(): void {
    this.hotelName = '';
    this.activeDate = '';
    this.onSearch();
  }
}