import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'app-ride-selector',
  templateUrl: './ride-selector.component.html',
  styleUrls: ['./ride-selector.component.css']
})
export class RideSelectorComponent {
  @Input() lines;
  @Output() lineSelect = new EventEmitter();

  @Input() line;

  @Input() rides;

  @Input() selectedRide: number;
  @Output() selectedRideChange = new EventEmitter<number>();

  
  constructor() { }

  selectLine(line) {
    this.lineSelect.emit(line);
  }

  prevRide() {
    this.selectedRide--;
    this.selectedRideChange.emit(this.selectedRide);
  }

  nextRide() {
    this.selectedRide++;
    this.selectedRideChange.emit(this.selectedRide);
  }

}