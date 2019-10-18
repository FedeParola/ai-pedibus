import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogUserLinesComponent } from './dialog-user-lines.component';

describe('DialogUserLinesComponent', () => {
  let component: DialogUserLinesComponent;
  let fixture: ComponentFixture<DialogUserLinesComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DialogUserLinesComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogUserLinesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
