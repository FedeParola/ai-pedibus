import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RideSelectorComponent } from './ride-selector.component';

describe('RideSelectorComponent', () => {
  let component: RideSelectorComponent;
  let fixture: ComponentFixture<RideSelectorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RideSelectorComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RideSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
