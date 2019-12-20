import { Component, OnInit, OnDestroy } from '@angular/core';
import { ReservationService } from '../reservation.service';
import { AuthenticationService } from '../authentication.service';
import { LineService } from '../line.service';
import { UsersService } from '../users.service';
import { Router } from '@angular/router';
import { MatSnackBar, MatDialog } from '@angular/material';
import { HttpErrorResponse } from '@angular/common/http';
import * as moment from 'moment';
import { PupilsDialogComponent } from './pupils-dialog/pupils-dialog.component';

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
  reservations: Map<number, any>;  // K: rideId, V: reservation, contains reservations for all lines
  reservationsSub;
  cantUpdateExplanation: string;

  constructor(private reservationService: ReservationService,
              private authenticationService: AuthenticationService,
              private lineService: LineService,
              private usersService: UsersService,
              private router: Router,
              private _snackBar: MatSnackBar,
              private dialog: MatDialog) { }

  ngOnInit() {
    this.pupilsSub = this.usersService.getPupils(this.authenticationService.getUsername()).subscribe(
      (res) => {
        this.pupils = res;

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
        
      },
      (error) => {
        this.handleError(error)
      }
    );

    this.lineService.getLines().subscribe(
      (res) => {
        this.lines = res;
        this.selectedLine = this.lines[0];
        this.loadLine();
        
      },
      (error) => {
        this.handleError(error)
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

  loadLine() {
    this.stops = null;
    this.selectedRideIndex = -1;
    this.rides = null;

    this.lineService.getStops(this.selectedLine.id).subscribe(
      (res) => {
        this.stops = res;
      },
      (error) => {
        this.handleError(error)
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
            this.selectedRideIndex = this.findRide(selRide);
          }

          // If prevoius current ride was removed or this is the first time displaying a ride
          // pick the closest ride with date >= current date
          if (this.selectedRideIndex == -1) {
            let curDate = moment().format('YYYY-MM-DD');
            let closestRide = this.rides.length-1;

            while (closestRide > 0 && this.rides[closestRide].date >= curDate) {
              closestRide--;
            }

            if (this.rides[closestRide].date < curDate && !(closestRide === this.rides.length-1)) {
              closestRide++;
            }
            this.selectedRideIndex = closestRide;
          }
        }
      },
      (error) => {
        this.handleError(error)
      }
    );
  }

  private findRide(ride): number {
    for (let [i, r] of this.rides.entries()) {
      if (ride.id == r.id) {
        return i;
      }
    }

    return -1;
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

    if (this.reservationsSub) {
      this.reservationsSub.unsubscribe();
    }
    this.reservationsSub = this.reservationService.getReservations(this.selectedPupil.id).subscribe(
      (res) => {
        this.reservations = res;
      },
      (error) => {
        this.handleError(error)
      }
    );
  }

  onStopClick(stopId: number) {
    let r = this.reservations.get(this.selectedRide.id);

    if (r) {
      if (stopId == r.stopId) {
        // Delete reservation
        this.reservationService.deleteReservation(r.id).subscribe(
          (res) => {
            this.reservations.delete(this.selectedRide.id)
          },
          (error) => {
            this.handleError(error);
          }
        );

      } else {
        // Change stop of reservation
        this.reservationService.updateReservationStop(r.id, stopId).subscribe(
          (res) => {
            r.stopId = stopId;
          },
          (error) => {
            this.handleError(error);
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
          this.reservations.set(this.selectedRide.id, r);
        },
        (error) => {
          this.handleError(error);
        }
      );
    }

    return false;  // Stop event propagation (change selected radio only on api req success)
  }

  cantUpdate(): boolean {
    let cantUpdate = false;
    this.cantUpdateExplanation = '';

    if (moment().isAfter(moment(this.selectedRide.date))) {
      cantUpdate = true;
      this.cantUpdateExplanation += "Time expired for this ride"
    }
    
    if (this.selectedReservation && this.selectedReservation.attendanceId !== undefined) {
      cantUpdate = true;
      if (this.cantUpdateExplanation != '') {
        this.cantUpdateExplanation += '\n';
      }
      this.cantUpdateExplanation += "Pupil already marked as present";
    }
    
    return cantUpdate;
  }

  private handleError(error: HttpErrorResponse) {
    if (!(error.error instanceof ErrorEvent) && error.status == 401) {
      // Not authenticated or auth expired
      this.router.navigateByUrl('/login');
    
    } else {
      // All other errors
      console.error("Error contacting server");
      this._snackBar.open("Error in the communication with the server!", "", { panelClass: 'error-snackbar', duration: 5000 });
    }
  }
}
