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
  currentDate;
  currentLine;
  reservations$: Observable<any>;
  lines;
  lineData$;
  availabilities;
  currentAvailability;
  nextEnabled;
  prevEnabled;

  constructor(private attendanceService: AttendanceService,
              private authenticationService: AuthenticationService,
              private router: Router,
              private _snackBar: MatSnackBar) {}

  ngOnInit() {
    this.attendanceService.getLines().subscribe((res) => {
      this.lines = res;
      this.currentLine = this.lines[0];
      this.lineData$ = this.attendanceService.getLine(this.currentLine.id);
      this.loadAvailabilities();
      
    },
    (error) => {
      this.handleError(error)
    });
  }

  loadAvailabilities() {
    this.availabilities = null;
    this.attendanceService.getAvailabilities(this.authenticationService.getUsername(), this.currentLine.id).subscribe(
      (res) => {
        this.availabilities = res;
        this.currentAvailability = 0;
        this.currentDate = undefined;
        this.currentDate = new Date(this.availabilities[this.currentAvailability].ride.date).toLocaleDateString("en-GB", {
          weekday: 'short',
          year: 'numeric',
          month: 'long',
          day: 'numeric'
        })

        this.prevEnabled = true;
        this.nextEnabled = true;
        if (this.currentAvailability == 0 || (this.currentAvailability == 1 && this.availabilities[0].date == this.availabilities[1].date)) {
          this.prevEnabled = false;
        }
        if (this.currentAvailability == this.availabilities.length-1 ||
            (this.currentAvailability == this.availabilities.length-2 &&
            this.availabilities[this.availabilities.length-1].date == this.availabilities[this.availabilities.length-2].date)) {
          this.nextEnabled = false;
        }
      },
      (error) => {
        this.handleError(error);
      }
    )
  }

  loadCurrentRide() {
    this.reservations$ = this.attendanceService.getReservations(this.currentLine, this.currentDate)
      .pipe(
        catchError((error) => {
          this.handleError(error);
          throw error; // Propagate error
        })
      )
  }

  onPupilClick(pupil, direction: string) {
    if (!pupil.disabled) {
      pupil.disabled = true;
      if(pupil.attendanceId >= 0){
        /*Remove attendance from the service*/
        this.attendanceService.deleteAttendance(this.currentLine, this.currentDate, pupil.attendanceId)
          .subscribe(
            () => {
              pupil.attendanceId = -1;
              pupil.disabled = false;
            },
            (error) => {
              this.handleError(error);
              pupil.disabled = false;
            }
          );

      } else {
        /*Add attendance on the service*/ 
        this.attendanceService.createAttendance(this.currentLine, this.currentDate, pupil.id, direction)
          .subscribe(
            (response: any) => {
              pupil.attendanceId = response.Id;
              pupil.disabled = false;
            },
            (error) => {
              this.handleError(error);
              pupil.disabled = false;
            }
          );
      }
    }
  }

  selectLine(line) {
    this.currentLine = line;
    this.lineData$ = this.attendanceService.getLine(this.currentLine.id);
    this.loadAvailabilities();
  }

  nextRide() {
    if (this.availabilities[this.currentAvailability+1].date == this.availabilities[this.currentAvailability].date) {
      this.currentAvailability += 2;
    } else {
      this.currentAvailability++;
    }

    this.currentDate = new Date(this.availabilities[this.currentAvailability].ride.date).toLocaleDateString("en-GB", {
      weekday: 'short',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
 
    // this.loadCurrentRide();
  
    this.prevEnabled = true;
    if (this.currentAvailability == this.availabilities.length-1 ||
        (this.currentAvailability == this.availabilities.length-2 &&
        this.availabilities[this.availabilities.length-1].date == this.availabilities[this.availabilities.length-2].date)) {
      this.nextEnabled = false;
    }
  }

  prevRide() {
    if (this.availabilities[this.currentAvailability-1].date == this.availabilities[this.currentAvailability].date) {
      this.currentAvailability -= 2;
    } else {
      this.currentAvailability--;
    }

    this.currentDate = new Date(this.availabilities[this.currentAvailability].ride.date).toLocaleDateString("en-GB", {
      weekday: 'short',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
 
    // this.loadCurrentRide();
    
    this.nextEnabled = true;
    if (this.currentAvailability == 0 || (this.currentAvailability == 1 && this.availabilities[0].date == this.availabilities[1].date)) {
      this.prevEnabled = false;
    }
  }



  private handleError(error: HttpErrorResponse) {
    if (!(error.error instanceof ErrorEvent) && error.status == 401) {
      /* Not authenticated or auth expired */
      this.router.navigateByUrl('/login');
    
    } else {
      /* All other errors*/
      console.error("Error contacting server");
      this._snackBar.open("Error in the communication with the server!", "", { panelClass: 'error-snackbar', duration: 5000 });
    }
  };
}