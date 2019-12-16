import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogPupilComponent } from './dialog-pupil.component';

describe('DialogPupilComponent', () => {
  let component: DialogPupilComponent;
  let fixture: ComponentFixture<DialogPupilComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DialogPupilComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogPupilComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
