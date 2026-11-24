import { DatePipe, DecimalPipe } from '@angular/common';

describe('Display Formatting Pipes', () => {
  // Verifies consistent date and currency-style display formatting.
  const decimalPipe = new DecimalPipe('en-US');
  const datePipe = new DatePipe('en-US');

  it('TC-FE-PIPE-01: DecimalPipe formats room rates and marked-up totals to 2 decimal places', () => {
    expect(decimalPipe.transform(180, '1.2-2')).toBe('180.00');
    expect(decimalPipe.transform(1450.5, '1.2-2')).toBe('1,450.50');
    expect(decimalPipe.transform(99.996, '1.2-2')).toBe('100.00');
  });

  it('TC-FE-PIPE-02: DecimalPipe handles zero and null values safely', () => {
    expect(decimalPipe.transform(0, '1.2-2')).toBe('0.00');
    expect(decimalPipe.transform(null, '1.2-2')).toBeNull();
  });

  it('TC-FE-PIPE-03: DatePipe formats ISO contract validity dates consistently', () => {
    expect(datePipe.transform('2026-12-01', 'yyyy-MM-dd')).toBe('2026-12-01');
    expect(datePipe.transform('2026-12-31', 'MMM d, y')).toBe('Dec 31, 2026');
  });
});