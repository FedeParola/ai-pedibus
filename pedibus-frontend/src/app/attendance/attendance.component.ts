import { Component, OnInit } from '@angular/core';
import { AttendanceService } from '../attendance.service';
import { AuthenticationService } from '../authentication.service';
import { Observable } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material';

@Component({
  selector: 'app-attendance',
  templateUrl: './attendance.component.html',
  styleUrls: ['./attendance.component.css']
})
export class AttendanceComponent implements OnInit {
  lines;
  selectedLine;
  stops;
  pupils;
  rides;
  selectedRideIndex = -1;
  rideData;
  missingPupils;

  constructor(private attendanceService: AttendanceService,
              private authenticationService: AuthenticationService,
              private router: Router,
              private _snackBar: MatSnackBar) {}

  ngOnInit() {
    this.attendanceService.getLines().subscribe(
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

  loadLine() {
    this.stops = null;
    this.pupils = null;
    this.selectedRideIndex = -1;
    this.rides = null;

    this.attendanceService.getStops(this.selectedLine.id).subscribe(
      (res) => {
        this.stops = res;
      },
      (error) => {
        this.handleError(error)
      }
    );

    this.attendanceService.getPupils(this.selectedLine.id).subscribe(
      (res) => {
        this.pupils = res;
      },
      (error) => {
        this.handleError(error)
      }
    );

    this.attendanceService.getRides(this.authenticationService.getUsername(), this.selectedLine.id).subscribe(
      (res) => {
        this.rides = res;

        // Pick the closest ride to current date
        this.selectedRideIndex = this.rides.length-2;

        this.loadRideData();
      },
      (error) => {
        this.handleError(error)
      }
    );
  }

  loadRideData() {
    this.attendanceService.getRideData(this.selectedRide.id).subscribe(
      (res) => {
        this.rideData = res;
        // Compute missing pupils
        this.missingPupils = [];
        for (let pupil of this.pupils) {
          if (!this.findPupil(pupil)) {
            this.missingPupils.push(pupil);
          }
        }
      },
      (error) => {
        this.handleError(error);
      }
    );
  }

  private findPupil(pupil) {
    for (let stopData of this.rideData.values()) {
      for (let data of stopData) {
        if (data.pupil.id == pupil.id) {
          return true;
        }
      }
    }

    return false;
  }

  onDataClick(data, i) {
    if (data.type == 'reservation') {
      console.log(data.stopId);
      this.attendanceService.createAttendance(data.pupil.id, this.selectedRide.id, data.stopId).subscribe(
        (res) => {
          data.attendanceId = res;
          data.type = 'both';
        },
        (error) => {
          this.handleError(error);
        }
      );

    } else {
      // Both other types represent an attendance, delete it
      this.attendanceService.deleteAttendance(data.attendanceId).subscribe(
        (res) => {
          if (data.type == 'attendance') {
            // Just deleted an attendance without reservation, delete data an add pupil to missing
            this.rideData.get(data.stopId).splice(i, 1);
            this.missingPupils.push(data.pupil)
          
          } else {
            // Just deleted an attendance with reservation, mark data as simple reservation
            // and remove attendance id
            data.type = 'reservation';
            delete data.attendanceId;
          }
        },
        (error) => {
          this.handleError(error);
        }
      );

    }
  }

  onPupilClick(pupil, i) {
    // For now create the reservation for the first stop
    let stopId = this.stops.outwardStops[0].id;
    this.attendanceService.createAttendance(pupil.id, this.selectedRide.id, stopId).subscribe(
      (res) => {
        // Add the attendance to rideData
        let data = {
          attendanceId: res,
          type: 'attendance',
          pupil: pupil,
          stopId: stopId
        };
        if (!this.rideData.has(stopId)) {
          this.rideData.set(stopId, []);
        }
        this.rideData.get(stopId).push(data);
        
        // Remove the pupil from missing ones
        this.missingPupils.splice(i, 1);
      },
      (error) => {
        this.handleError(error);
      }
    );
  }

  canNextRide() {
    if (this.selectedRideIndex == -1) {
      return false;
    }

    if (this.selectedRideIndex == this.rides.length-1 ||
        (this.selectedRideIndex == this.rides.length-2 &&
         this.rides[this.rides.length-1].date == this.rides[this.rides.length-2].date)) {
      return false;
    }

    return true;
  }

  nextRide() {
    if (this.rides[this.selectedRideIndex+1].date == this.selectedRide.date) {
      this.selectedRideIndex += 2;
    } else {
      this.selectedRideIndex++;
    }
 
    this.loadRideData();
  }

  canPrevRide() {
    if (this.selectedRideIndex == -1) {
      return false;
    }
    
    if (this.selectedRideIndex == 0 ||
        (this.selectedRideIndex == 1 && this.rides[0].date == this.rides[1].date)) {
      return false;
    }

    return true;
  }

  prevRide() {
    if (this.rides[this.selectedRideIndex-1].date == this.selectedRide.date) {
      this.selectedRideIndex -= 2;
    } else {
      this.selectedRideIndex--;
    }

    this.loadRideData();
  }

  canOutbound() {
    if (this.selectedRide.direction == 'O') {
      return true;
    } else if (this.selectedRideIndex == 0) {
      return false;
    } else if (this.selectedRide.date == this.rides[this.selectedRideIndex-1].date) {
      return true;
    } else {
      return false;
    }
  }

  canReturn() {
    if (this.selectedRide.direction == 'R') {
      return true;
    } else if (this.selectedRideIndex == this.rides.length-1) {
      return false;
    } else if (this.selectedRide.date == this.rides[this.selectedRideIndex+1].date) {
      return true;
    } else {
      return false;
    }
  }

  // 0 = 'O', 1 = 'R'
  changeDirection(direction) {
    // Check if the ride has already changed
    if (direction == 0 && this.selectedRide.direction == 'O' ||
        direction == 1 && this.selectedRide.direction == 'R') {
      return;
    }

    if (direction == 0) {
      this.selectedRideIndex--;
    } else {
      this.selectedRideIndex++;
    }

    this.loadRideData();
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