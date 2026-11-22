import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';

describe('Sun Travels Reactive Forms', () => {
  let fb: FormBuilder;

  function buildRoomTypeGroup(): FormGroup {
    return fb.group({
      typeName: ['', Validators.required],
      price: [null, [Validators.required, Validators.min(0.01)]],
      noOfRooms: [1, [Validators.required, Validators.min(1)]],
      maxAdults: [1, [Validators.required, Validators.min(1)]]
    });
  }

  function buildContractForm(): FormGroup {
    return fb.group({
      hotelName: ['', Validators.required],
      validFrom: ['', Validators.required],
      validTo: ['', Validators.required],
      markup: [15, [Validators.required, Validators.min(0)]],
      roomTypes: fb.array([buildRoomTypeGroup()])
    });
  }

  function buildSearchForm(): FormGroup {
    return fb.group({
      checkInDate: ['', Validators.required],
      noOfNights: [1, [Validators.required, Validators.min(1)]],
      roomRequests: fb.array([
        fb.group({
          noOfRooms: [1, [Validators.required, Validators.min(1)]],
          noOfAdults: [2, [Validators.required, Validators.min(1)]]
        })
      ])
    });
  }

  beforeEach(() => {
    fb = new FormBuilder();
  });

  it('TC-FE-FORM-01: Contract form starts invalid when hotelName and validity dates are empty', () => {
    const form = buildContractForm();
    expect(form.valid).toBe(false);
    expect(form.get('hotelName')?.hasError('required')).toBe(true);
    expect(form.get('validFrom')?.hasError('required')).toBe(true);
    expect(form.get('validTo')?.hasError('required')).toBe(true);
  });

  it('TC-FE-FORM-02: Contract form enforces minimum bounds on markup, price, noOfRooms, and maxAdults', () => {
    const form = buildContractForm();
    const roomArray = form.get('roomTypes') as FormArray;

    form.get('markup')?.setValue(-5);
    roomArray.at(0).patchValue({
      typeName: 'Standard',
      price: 0,
      noOfRooms: 0,
      maxAdults: 0
    });

    expect(form.get('markup')?.hasError('min')).toBe(true);
    expect(roomArray.at(0).get('price')?.hasError('min')).toBe(true);
    expect(roomArray.at(0).get('noOfRooms')?.hasError('min')).toBe(true);
    expect(roomArray.at(0).get('maxAdults')?.hasError('min')).toBe(true);
  });

  it('TC-FE-FORM-03: Supports dynamically adding and removing RoomType FormGroups in FormArray', () => {
    const form = buildContractForm();
    const roomArray = form.get('roomTypes') as FormArray;

    expect(roomArray.length).toBe(1);
    roomArray.push(buildRoomTypeGroup());
    expect(roomArray.length).toBe(2);

    roomArray.removeAt(1);
    expect(roomArray.length).toBe(1);
  });

  it('TC-FE-FORM-04: Contract form becomes valid when all required fields and room rows are populated', () => {
    const form = buildContractForm();
    form.patchValue({
      hotelName: 'Jetwing Lighthouse',
      validFrom: '2026-12-01',
      validTo: '2026-12-31',
      markup: 15
    });

    const roomArray = form.get('roomTypes') as FormArray;
    roomArray.at(0).patchValue({
      typeName: 'Ocean View',
      price: 165,
      noOfRooms: 8,
      maxAdults: 2
    });

    expect(form.valid).toBe(true);
  });

  it('TC-FE-FORM-05: Agent search form validates checkInDate, nights >= 1, and roomRequests array', () => {
    const searchForm = buildSearchForm();
    expect(searchForm.valid).toBe(false);

    searchForm.patchValue({
      checkInDate: '2026-12-15',
      noOfNights: 3
    });

    expect(searchForm.valid).toBe(true);
  });
});