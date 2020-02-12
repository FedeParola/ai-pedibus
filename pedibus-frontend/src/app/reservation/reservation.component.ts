import { Component, OnInit, OnDestroy } from '@angular/core';
import { MatSnackBar, MatDialog } from '@angular/material';
import * as moment from 'moment';

import { PupilsDialogComponent } from './pupils-dialog/pupils-dialog.component';
import { AttendanceService } from '../attendance.service';
import { ReservationService } from '../reservation.service';
import { AuthenticationService } from '../authentication.service';
import { LineService } from '../line.service';
import { UsersService } from '../users.service';
import { AppComponent } from '../app.component';
import { handleError, findElement, findClosestRide } from '../utils';

@Component({
  selector: 'app-reservation',
  templateUrl: './reservation.component.html',
  styleUrls: ['./reservation.component.css']
})
export class ReservationComponent implements OnInit, OnDestroy {
  pupils;
  pupilsSub;
  selectedPupil;
  lines;
  selectedLine;
  stops;
  rides;
  ridesSub;
  selectedRideIndex = -1;
  reservations: Map<number, any>;  // K: rideId, V: reservation; contains reservations for all lines
  reservationsSub;
  attendances: Map<number, any>;  // K: rideId, V: attendance; contains attendances for all lines
  attendancesSub;
  cantUpdateExplanations: string[];  // K: stopId, V: why can't select that stop

  constructor(private attendanceService: AttendanceService,
              private reservationService: ReservationService,
              private authenticationService: AuthenticationService,
              private lineService: LineService,
              private usersService: UsersService,
              private _snackBar: MatSnackBar,
              private dialog: MatDialog,
              private appComponent: AppComponent) { 
                this.appComponent.selectedView="Reservations"
              }

  ngOnInit() {
    this.cantUpdateExplanations = [];

    this.pupilsSub = this.usersService.getPupils(this.authenticationService.getUsername()).subscribe(
      (res) => {
        this.pupils = res;

        if (this.pupils.length > 0) {
          if (this.selectedPupil) {
            // Select the old pupil if still present
            for (let p of this.pupils) {
              if (p.id === this.selectedPupil.id) {
                this.selectedPupil = p;
                return;
              }
            }
          }
          
          this.selectedPupil = this.pupils[0];
          this.loadPupil();
        }
        
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );

    this.lineService.getLines().subscribe(
      (res) => {
        this.lines = res;
        if (this.selectedPupil) {
          let line = this.findLine(this.selectedPupil.lineId);
          if (line) {
            this.selectLine(line);
          } else {
            this.selectLine(this.lines[0]);
          }
        }
        
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  ngOnDestroy(): void {
    if (this.pupilsSub) {
      this.pupilsSub.unsubscribe();
    }
    if (this.ridesSub) {
      this.ridesSub.unsubscribe();
    }
    if (this.reservationsSub) {
      this.reservationsSub.unsubscribe();
    }
    if (this.attendancesSub) {
      this.attendancesSub.unsubscribe();
    }
  }

  get selectedRide() {
    return this.selectedRideIndex == -1 ? null : this.rides[this.selectedRideIndex];
  }

  selectLine(line) {
    this.selectedLine = line;
    this.loadLine();
  }

  get selectedReservation() {
    return this.reservations.get(this.selectedRide.id);
  }

  get selectedAttendance() {
    return this.attendances.get(this.selectedRide.id);
  }

  loadLine() {
    this.stops = null;
    this.selectedRideIndex = -1;
    this.rides = null;

    this.lineService.getStops(this.selectedLine.id).subscribe(
      (res) => {
        this.stops = res;
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );

    if (this.ridesSub) {
      this.ridesSub.unsubscribe();
    }
    this.ridesSub = this.lineService.getRides(this.selectedLine.id).subscribe(
      (res) => {
        let selRide = this.selectedRide;

        this.selectedRideIndex = -1;
        this.rides = res;

        if (this.rides.length > 0) {
          if (selRide) {
            // Check if current ride is present in the new list,
            // if it is stay on that ride
            this.selectedRideIndex = findElement(selRide, this.rides);
          }

          // If prevoius current ride was removed or this is the first time displaying a ride
          // pick the closest ride with date >= current date
          if (this.selectedRideIndex == -1) {
            this.selectedRideIndex = findClosestRide(this.rides);
          }
        }
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  changePupil() {
    // Display the dialog for pupil selection
    const dialogRef = this.dialog.open(PupilsDialogComponent, {data: this.pupils});

    dialogRef.afterClosed().subscribe(pupil => {
      if (pupil) {
        this.selectedPupil = pupil;
        this.loadPupil();
      }
    });
  }

  loadPupil() {
    this.reservations = null;
    this.attendances = null;

    if (this.reservationsSub) {
      this.reservationsSub.unsubscribe();
    }
    this.reservationsSub = this.reservationService.getReservations(this.selectedPupil.id).subscribe(
      (res) => {
        this.reservations = res;
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );

    if (this.attendancesSub) {
      this.attendancesSub.unsubscribe();
    }
    this.attendancesSub = this.attendanceService.getAttendancesByPupil(this.selectedPupil.id).subscribe(
      (res) => {
        this.attendances = res;
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
    
    if (this.lines) {
      let line = this.findLine(this.selectedPupil.lineId);
      if (line) {
        this.selectLine(line);
      } else {
        this.selectLine(this.lines[0]);
      }
    }
  }

  findLine(lineId: number) {
    for (let line of this.lines) {
      if (line.id == lineId) {
        return line;
      }
    }

    return undefined;
  }

  onStopClick(stopId: number) {
    let reservations = this.reservations;
    let r = this.selectedReservation;

    if (r) {
      if (stopId == r.stopId) {
        // Delete reservation
        this.reservationService.deleteReservation(r.id).subscribe(
          (res) => {
            reservations.delete(this.selectedRide.id)
          },
          (error) => {
            handleError(error, this._snackBar);
          }
        );

      } else {
        // Change stop of reservation
        this.reservationService.updateReservationStop(r.id, stopId).subscribe(
          (res) => {
            r.stopId = stopId;
          },
          (error) => {
            handleError(error, this._snackBar);
          }
        );

      }

    } else {
      // Create reservation
      this.reservationService.createReservation(
        this.selectedPupil.id,
        this.selectedRide.id,
        stopId
      ).subscribe(
        (res) => {
          r = {
            id: res,
            rideId: this.selectedRide.id,
            stopId: stopId
          }
          reservations.set(this.selectedRide.id, r);
        },
        (error) => {
          handleError(error, this._snackBar);
        }
      );
    }

    return false;  // Stop event propagation (change selected radio only on api req success)
  }

  cantUpdate(stop): boolean {
    let cantUpdate = false;
    this.cantUpdateExplanations[stop.id] = '';

    // Check if time of the given stop has exceeded
    let time = stop.time.split(':');
    if (moment().isAfter(moment(this.selectedRide.date).hour(time[0]).minute(time[1]))) {
      cantUpdate = true;
      this.cantUpdateExplanations[stop.id] += "Time expired for this stop";
    }
    
    // Check if pupil is already marked as present
    if (this.selectedAttendance) {
      cantUpdate = true;
      if (this.cantUpdateExplanations[stop.id] != '') {
        this.cantUpdateExplanations[stop.id] += '\n';
      }
      this.cantUpdateExplanations[stop.id] += "Pupil already marked as present";
    }
    
    return cantUpdate;
  }
}
