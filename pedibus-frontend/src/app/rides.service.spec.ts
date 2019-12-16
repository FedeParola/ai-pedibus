import { TestBed } from '@angular/core/testing';

import { RidesService } from './rides.service';

describe('RidesService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: RidesService = TestBed.get(RidesService);
    expect(service).toBeTruthy();
  });
});
