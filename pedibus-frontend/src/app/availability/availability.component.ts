import { Component, OnInit } from '@angular/core';
import { AvailabilityService } from '../availability.service';
import { AuthenticationService } from '../authentication.service';
import { LineService } from '../line.service';
import { Router } from '@angular/router';
import { MatSnackBar, MatDialog } from '@angular/material';
import { HttpErrorResponse } from '@angular/common/http';
import * as moment from 'moment';

import { DeletionConfirmDialogComponent } from './deletion-confirm-dialog/deletion-confirm-dialog.component';

@Component({
  selector: 'app-availability',
  templateUrl: './availability.component.html',
  styleUrls: ['./availability.component.css']
})
export class AvailabilityComponent implements OnInit {
  lines;
  selectedLine;
  stops;
  rides;
  selectedRideIndex = -1;
  availabilities: Map<number, any>;  // K: rideId, V: availability
  cantUpdateExplanation: string;

  constructor(private availabilityService: AvailabilityService,
              private authenticationService: AuthenticationService,
              private lineService: LineService,
              private router: Router,
              private _snackBar: MatSnackBar,
              private dialog: MatDialog) { }

  ngOnInit() {
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
        this.handleError(error)
      }
    );

    this.lineService.getRides(this.selectedLine.id).subscribe(
      (res) => {
        this.rides = res;

        if (this.rides.length > 0) {
          // Pick the closest ride with date >= current date
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
      },
      (error) => {
        this.handleError(error)
      }
    );

    this.availabilityService.getAvailabilities(this.authenticationService.getUsername(), this.selectedLine.id).subscribe(
      (res) => {
        this.availabilities = res;
      },
      (error) => {
        this.handleError(error);
      }
    );
  }

  onStopClick(stopId: number) {
    let a = this.availabilities.get(this.selectedRide.id);

    if (a) {
      if (stopId == a.stopId) {
        // Delete availability
        // Display the dialog for confirmation
        const dialogRef = this.dialog.open(DeletionConfirmDialogComponent);

        dialogRef.afterClosed().subscribe(conf => {
          if (conf) {
            this.availabilityService.deleteAvailability(a.id).subscribe(
              (res) => {
                this.availabilities.delete(this.selectedRide.id)
              },
              (error) => {
                this.handleError(error);
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
            this.handleError(error);
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
          this.availabilities.set(this.selectedRide.id, a);
        },
        (error) => {
          this.handleError(error);
        }
      );
    }

    return false;  // Stop event propagation (change selected radio only on api req success)
  }

  confirm() {
    this.availabilityService.confirmAvailability(this.selectedAvailability.id).subscribe(
      (res) => {
        this.selectedAvailability.status = 'CONFIRMED';
      },
      (error) => {
        this.handleError(error);
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

  private handleError(error: HttpErrorResponse) {
    if (!(error.error instanceof ErrorEvent) && error.status == 401) {
      // Not authenticated or auth expired
      this.router.navigateByUrl('/login');
    
    } else {
      // All other errors
      console.error("Error contacting server");
      this._snackBar.open("Error in the communication with the server!", "", { panelClass: 'error-snackbar', duration: 5000 });
    }
  };
}
