import { Component, OnInit, OnDestroy } from '@angular/core';
import { MatSnackBar, MatDialog } from '@angular/material';
import * as moment from 'moment';

import { AvailabilityService } from '../availability.service';
import { AuthenticationService } from '../authentication.service';
import { LineService } from '../line.service';
import { AppComponent } from '../app.component';
import { DeletionConfirmDialogComponent } from './deletion-confirm-dialog/deletion-confirm-dialog.component';
import { handleError, findElement, findNextClosestRide } from '../utils';

@Component({
  selector: 'app-availability',
  templateUrl: './availability.component.html',
  styleUrls: ['./availability.component.css']
})
export class AvailabilityComponent implements OnInit, OnDestroy {
  lines;
  selectedLine;
  stops;
  rides;
  ridesSub;
  selectedRideIndex = -1;
  availabilities: Map<number, any>;  // K: rideId, V: availability
  availabilitiesSub;
  cantUpdateExplanation: string;

  constructor(private availabilityService: AvailabilityService,
              private authenticationService: AuthenticationService,
              private lineService: LineService,
              private _snackBar: MatSnackBar,
              private dialog: MatDialog,
              private appComponent: AppComponent) {
                this.appComponent.selectedView="Availabilities";
               }

  ngOnInit() {
    this.lineService.getLines().subscribe(
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

  ngOnDestroy(): void {
    // Unsubscribe from all events
    if (this.ridesSub) {
      this.ridesSub.unsubscribe();
    }
    if (this.availabilitiesSub) {
      this.availabilitiesSub.unsubscribe();
    }
  }

  get selectedRide() {
    return this.selectedRideIndex == -1 ? null : this.rides[this.selectedRideIndex];
  }

  selectLine(line) {
    this.selectedLine = line;
    this.loadLine();
  }

  get selectedAvailability() {
    return this.availabilities.get(this.selectedRide.id);
  }

  loadLine() {
    this.stops = null;
    this.selectedRideIndex = -1;
    this.rides = null;
    this.availabilities = null;

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
            this.selectedRideIndex = findNextClosestRide(this.rides);
          }
        }
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );

    if (this.availabilitiesSub) {
      this.availabilitiesSub.unsubscribe();
    }
    this.availabilitiesSub = this.availabilityService.getAvailabilities(
      this.authenticationService.getUsername(),
      this.selectedLine.id
    ).subscribe(
      (res) => {
        this.availabilities = res;
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  onStopClick(stopId: number) {
    let availabilities = this.availabilities;
    let a = this.selectedAvailability;

    if (a) {
      if (stopId == a.stopId) {
        // Delete availability
        // Display the dialog for confirmation
        const dialogRef = this.dialog.open(DeletionConfirmDialogComponent);

        dialogRef.afterClosed().subscribe(conf => {
          if (conf) {
            this.availabilityService.deleteAvailability(a.id).subscribe(
              (res) => {
                availabilities.delete(this.selectedRide.id)
              },
              (error) => {
                handleError(error, this._snackBar);
              }
            );
          }
        });

      } else {
        // Change stop of availability
        this.availabilityService.updateAvailabilityStop(a.id, stopId).subscribe(
          (res) => {
            a.stopId = stopId;
          },
          (error) => {
            handleError(error, this._snackBar);
          }
        );

      }

    } else {
      // Create availability
      this.availabilityService.createAvailability(
        this.authenticationService.getUsername(),
        this.selectedRide.id,
        stopId
      ).subscribe(
        (res) => {
          a = {
            id: res,
            stopId: stopId,
            status: 'NEW'
          }
          availabilities.set(this.selectedRide.id, a);
        },
        (error) => {
          handleError(error, this._snackBar);
        }
      );
    }

    return false;  // Stop event propagation (change selected radio only on api req success)
  }

  confirm() {
    let a = this.selectedAvailability;
    this.availabilityService.confirmAvailability(a.id).subscribe(
      (res) => {
        a.status = 'CONFIRMED';
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  cantUpdate(): boolean {
    let cantUpdate = false;
    this.cantUpdateExplanation = '';

    if (moment().isAfter(moment(this.selectedRide.date).subtract(1, 'days').hour(18))) {
      cantUpdate = true;
      this.cantUpdateExplanation += "Time expired for this ride"
    }
    
    if (this.selectedRide.consolidated) {
      cantUpdate = true;
      if (this.cantUpdateExplanation != '') {
        this.cantUpdateExplanation += '\n';
      }
      this.cantUpdateExplanation += "This ride is consolidated"
    
    } else if (this.selectedAvailability && this.selectedAvailability.status == 'CONFIRMED') {
      cantUpdate = true;
      if (this.cantUpdateExplanation != '') {
        this.cantUpdateExplanation += '\n';
      }
      this.cantUpdateExplanation += "You already confirmed your availability for this ride";
    }
    
    return cantUpdate;
  }
}
