import { Component, OnInit } from '@angular/core';
import { RidesService } from '../rides.service';
import { AuthenticationService } from '../authentication.service';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material';
import { LineService } from '../line.service';
import * as moment from 'moment';
import { AppComponent } from '../app.component';

import { handleError } from '../utils';

@Component({
  selector: 'app-rides',
  templateUrl: './rides.component.html',
  styleUrls: ['./rides.component.css']
})
export class RidesComponent implements OnInit {
  currentDirection = null;
  currentDate = null;
  lines = null;
  selectedLine = null;
  stops = null;
  selectedRide = null;
  availabilities = null;
  currRideSub;
  availabilitiesSub;

  constructor(private rideService: RidesService,
              private authenticationService: AuthenticationService,
              private lineService: LineService,
              private _snackBar: MatSnackBar,
              private appComponent: AppComponent) {
                this.appComponent.selectedView="Rides";
              }

  ngOnInit() {
    this.rideService.getMyLines(this.authenticationService.getUsername()).subscribe(
      (res) => {
        this.lines = res;
        this.selectedLine = this.lines[0];
        this.loadLine();
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  loadLine() {
    this.stops = null;
    this.selectedRide = null;
    this.availabilities = null;

    this.lineService.getStops(this.selectedLine.id).subscribe(
      (res) => {
        this.stops = res;
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );

    // Get the ride of the current day for the selected line  (for the outward direction)
    this.currentDate = new Date();
    this.loadRide('O');
  }

  loadRide(direction: string){
    if(direction.localeCompare('O') === 0)
      this.currentDirection = 'O';
    else
      this.currentDirection = 'R';
    let date = this.currentDate.getFullYear()+'-'+(this.currentDate.getMonth()+1)+'-'+this.currentDate.getDate();
    if (this.currRideSub) {
      this.currRideSub.unsubscribe();
    }
    this.currRideSub = this.rideService.getRide(this.selectedLine.id, date, direction).subscribe(
      (res) => {
        this.selectedRide = res[0];
        if(this.selectedRide != undefined){
          this.loadRideAvailabilities();
        }
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }
  
  loadRideAvailabilities() {
    this.availabilities = null;
    if (this.availabilitiesSub) {
      this.availabilitiesSub.unsubscribe();
    }
    this.availabilitiesSub = this.rideService.getRideAvailabilities(this.selectedRide.id).subscribe(
      (res) => {
        this.availabilities = res;
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  selectLine(line) {
    this.selectedLine = line;
    this.loadLine();
  }

  nextRide() {
    if(this.currentDirection.localeCompare('O') === 0){
      this.loadRide('R');
    } else {
      this.currentDate.setDate(this.currentDate.getDate() + 1);
      this.loadRide('O');
    }
  }

  prevRide() {
    if(this.currentDirection.localeCompare('R') === 0){
      this.loadRide('O');
    } else {
      this.currentDate.setDate(this.currentDate.getDate() - 1);
      this.loadRide('R');
    }
  }

  isChipDisabled(stop): boolean {
    if(this.selectedRide.consolidated)
      return true;
    else {
      // Disable the chip if current time exceed stop time of more than 30 mins 
      if (stop) {
        let treshold = moment(this.selectedRide.date+'T'+stop.time).add(30, 'minutes');
        return moment().isAfter(treshold);

      } else {
        let treshold = moment(this.selectedRide.date);
        return moment().isAfter(treshold, 'day');
      }
    }
  }  

  onAvailabilityClick(availability) {
    let newStatus: string;
    if(availability.status == 'NEW')
      //NEW -> ASSIGNED
      newStatus = 'ASSIGNED';
    else if(availability.status == 'CONSOLIDATED')
      console.log('Something wrong, should not enter this function if availability is consolidated!')
    else
      //CONFIRMED o ASSIGNED -> NEW
      newStatus = 'NEW';
    //SE CONSOLIDATED NON DOVREI MAI ENTRARE IN QUESTA FUNZIONE
    this.rideService.updateAvailability(availability.id, newStatus).subscribe(
      () => {
        availability.status = newStatus;
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  createRide(){
    let date = this.currentDate.getFullYear()+'-'+(this.currentDate.getMonth()+1)+'-'+this.currentDate.getDate();
    this.rideService.createRide(date, this.selectedLine.id, this.currentDirection).subscribe(
      (res) => {
        this.loadRide(this.currentDirection);
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  changeRideStatus(){
    this.rideService.changeRideStatus(this.selectedRide.id, !this.selectedRide.consolidated).subscribe(
      () => {
        this.loadRide(this.selectedRide.direction);
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  deleteRide(){
    this.rideService.deleteRide(this.selectedRide.id).subscribe(
      () => {
        this.selectedRide = undefined;
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  // private handleError(error: HttpErrorResponse) {
  //   if (!(error.error instanceof ErrorEvent) && error.status == 401) {
  //     // Not authenticated or auth expired
  //     this.authenticationService.logout();
    
  //   } else {
  //     // All other errors
  //     console.error("Error contacting server");
  //     this._snackBar.open("Error in the communication with the server!", "", { panelClass: 'error-snackbar', duration: 5000 });
  //   }
  // };
}