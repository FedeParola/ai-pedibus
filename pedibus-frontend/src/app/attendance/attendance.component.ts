import { Component, OnInit } from '@angular/core';
import { AttendanceService } from '../attendance.service';
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
  currentDate: Date;
  currentLine: string;
  reservations$: Observable<any>;
  lines: string[];

  constructor(private attendanceService: AttendanceService,
              private router: Router,
              private _snackBar: MatSnackBar) {}

  ngOnInit() {
    this.currentDate = new Date();
    this.attendanceService.getLines().subscribe((res: string[]) => {
      this.lines = res;
      this.currentLine = this.lines[0];
      this.loadCurrentRoute();
    },
    (error) => {
      this.handleError(error)
    });
  }

  loadCurrentRoute() {
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

  selectLine(line: string) {
    this.currentLine = line;
    this.loadCurrentRoute();
  }

  nextRoute() {
    this.currentDate = new Date(this.currentDate.valueOf()+24*60*60*1000);
    this.loadCurrentRoute();
  }

  prevRoute() {
    this.currentDate = new Date(this.currentDate.valueOf()-24*60*60*1000);
    this.loadCurrentRoute();
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