import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SimpleRideSelectorComponent } from './simple-ride-selector.component';

describe('SimpleRideSelectorComponent', () => {
  let component: SimpleRideSelectorComponent;
  let fixture: ComponentFixture<SimpleRideSelectorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SimpleRideSelectorComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SimpleRideSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
