import { Component, EventEmitter, Input, Output } from '@angular/core';
import { AppComponent } from '../app.component';

@Component({
  selector: 'app-simple-ride-selector',
  templateUrl: './simple-ride-selector.component.html',
  styleUrls: ['./simple-ride-selector.component.css']
})
export class SimpleRideSelectorComponent {
  @Input() lines;
  @Output() lineSelect = new EventEmitter();

  @Input() line;

  @Input() ride;
  @Output() prevRideClick = new EventEmitter();
  @Output() nextRideClick = new EventEmitter();
  @Input() prevRideDisabled: boolean;
  @Input() nextRideDisabled: boolean;

  title;
  
  constructor(private appComponent: AppComponent) { 
    this.title=appComponent.selectedView;
  }

  selectLine(line) {
    this.lineSelect.emit(line);
  }

  onPrevRideClick() {
    this.prevRideClick.emit();
  }

  onNextRideClick() {
    this.nextRideClick.emit();
  }
}