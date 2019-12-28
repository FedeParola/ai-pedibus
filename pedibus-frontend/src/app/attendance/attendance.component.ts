import { Component, OnInit, OnDestroy } from '@angular/core';
import { MatSnackBar, MatDialog } from '@angular/material';
import * as moment from 'moment';

import { AttendanceService } from '../attendance.service';
import { AuthenticationService } from '../authentication.service';
import { LineService } from '../line.service';
import { StopDialogComponent } from './stop-dialog/stop-dialog.component';
import { AppComponent } from '../app.component';
import { handleError, findElement, findClosestRide } from '../utils';


@Component({
  selector: 'app-attendance',
  templateUrl: './attendance.component.html',
  styleUrls: ['./attendance.component.css']
})
export class AttendanceComponent implements OnInit, OnDestroy {
  lines;
  selectedLine;
  stops;
  pupils;
  pupilsSub;
  rides;
  ridesSub;
  selectedRideIndex = -1;
  rideData;
  rideDataSub;
  missingPupils;


  private setting = {
    element: {
      dynamicDownload: null as HTMLElement
    }
  }

  constructor(private attendanceService: AttendanceService,
              private authenticationService: AuthenticationService,
              private lineService: LineService,
              private _snackBar: MatSnackBar,
              private dialog: MatDialog,
              private appComponent: AppComponent) {
                this.appComponent.selectedView="Attendances";
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
    if (this.pupilsSub) {
      this.pupilsSub.unsubscribe();
    }
    if (this.ridesSub) {
      this.ridesSub.unsubscribe();
    }
    if (this.rideDataSub) {
      this.rideDataSub.unsubscribe();
    }
  }

  downloadJSON(){
    this.dyanmicDownloadByHtmlTag({
      fileName: 'ride'+this.selectedRide.date+this.selectedRide.direction+'.json',
      text: this.exportoJSON()
    });
  }

  downloadCSV(){
    this.dyanmicDownloadByHtmlTag({
      fileName: 'ride'+this.selectedRide.date+this.selectedRide.direction+'.csv',
      text: this.exportToCsv()
    });
  }

  get selectedRide() {
    return this.selectedRideIndex == -1 ? null : this.rides[this.selectedRideIndex];
  }

  selectLine(line) {
    this.selectedLine = line;
    this.loadLine();
  }

  onSelectedRideChange(newRideIndex: number) {
    this.selectedRideIndex = newRideIndex;
    this.loadRideData();
  }

  loadLine() {
    this.stops = null;
    this.pupils = null;
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

    if (this.pupilsSub) {
      this.pupilsSub.unsubscribe();
    }
    this.pupilsSub = this.lineService.getPupils(this.selectedLine.id).subscribe(
      (res) => {
        this.pupils = res;
        if (this.rideData) {
          this.computeMissingPupils();
        }
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );

    if (this.ridesSub) {
      this.ridesSub.unsubscribe();
    }
    this.ridesSub = this.attendanceService.getRides(
      this.authenticationService.getUsername(),
      this.selectedLine.id
    ).subscribe(
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
            this.loadRideData();
          }
        }
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  loadRideData() {
    if (this.rideDataSub) {
      this.rideDataSub.unsubscribe();
    }
    this.rideDataSub = this.attendanceService.getRideData(this.selectedRide.id).subscribe(
      (res) => {
        this.rideData = res;
        if (this.pupils) {
          this.computeMissingPupils();
        }
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  isChipDisabled(stop?): boolean {
    // Disable the chip if current time exceed stop time of more than 30 mins 
    if (stop) {
      let treshold = moment(this.selectedRide.date+'T'+stop.time).add(30, 'minutes');
      return moment().isAfter(treshold);

    } else {
      let treshold = moment(this.selectedRide.date);
      return moment().isAfter(treshold, 'day');
    }
  }

  private computeMissingPupils() {
    this.missingPupils = [];
    for (let pupil of this.pupils) {
      if (!this.findPupil(pupil)) {
        this.missingPupils.push(pupil);
      }
    }
    this.missingPupils.sort((a, b) => a.name.localeCompare(b.name));
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
          handleError(error, this._snackBar);
        }
      );

    } else {
      // Both other types represent an attendance, delete it
      let rideData = this.rideData;
      this.attendanceService.deleteAttendance(data.attendanceId).subscribe(
        () => {
          if (data.type == 'attendance') {
            // Just deleted an attendance without reservation, delete data an add pupil to missing
            rideData.get(data.stopId).splice(i, 1);
            this.computeMissingPupils();
          
          } else {
            // Just deleted an attendance with reservation, mark data as simple reservation
            // and remove attendance id
            data.type = 'reservation';
            delete data.attendanceId;
          }
        },
        (error) => {
          handleError(error, this._snackBar);
        }
      );

    }
  }

  onPupilClick(pupil, i) {
    // Select the right set of stops to choose from and remove the School stop
    let sStops = this.selectedRide.direction == 'O' ? this.stops.outwardStops : this.stops.returnStops;
    if (sStops[0].name.toLowerCase() === 'school') {
      sStops = sStops.slice(1);
    } else if (sStops[sStops.length-1].name.toLowerCase() === 'school') {
      sStops = sStops.slice(0, -1);
    }

    // Display the dialog for stop selection
    const dialogRef = this.dialog.open(StopDialogComponent, {
      data: {
        pupil: pupil,
        stops: sStops
      }
    });

    let rideData = this.rideData;
    dialogRef.afterClosed().subscribe(stop => {
      if (stop) {
        this.attendanceService.createAttendance(pupil.id, this.selectedRide.id, stop.id).subscribe(
          (res) => {
            // Add the attendance to rideData
            let data = {
              attendanceId: res,
              type: 'attendance',
              pupil: pupil,
              stopId: stop.id
            };
            if (!this.rideData.has(stop.id)) {
              rideData.set(stop.id, []);
            }
            let pupilsData = rideData.get(stop.id);
            pupilsData.push(data);
            pupilsData.sort((a, b) => a.pupil.name.localeCompare(b.pupil.name));
            
            // Remove the pupil from missing ones
            this.computeMissingPupils();
          },
          (error) => {
            handleError(error, this._snackBar);
          }
        );
      }
    });
  }

  private dyanmicDownloadByHtmlTag(arg: {
    fileName: string,
    text: string
  }) {
    if (!this.setting.element.dynamicDownload) {
      this.setting.element.dynamicDownload = document.createElement('a');
    }
    const element = this.setting.element.dynamicDownload;
    const fileType = arg.fileName.indexOf('.json') > -1 ? 'text/json' : 'text/csv';
    element.setAttribute('href', `data:${fileType};charset=utf-8,${encodeURIComponent(arg.text)}`);
    element.setAttribute('download', arg.fileName);

    var event = new MouseEvent("click");
    element.dispatchEvent(event);
  }

  private exportoJSON(){
    var o = {};
    o["line"] = this.selectedLine.name;
    o["date"] = this.selectedRide.date;
    if(this.selectedRide.direction == 'O'){
      o["direction"] = "Outbound";
    }else{
      o["direction"] = "Return";
    }
    o["stops"] = []; 

    for(let stop of (this.selectedRide.direction == 'O' ? this.stops.outwardStops : this.stops.returnStops)){
      var s = {};
      s["time"] = stop.time;
      s["name"] = stop.name;

      var stopPupils = this.rideData.get(stop.id);
      if(stopPupils != undefined){
        s["pupils"] = [];
        for(let data of stopPupils){
          if(data.type == "both" || data.type == "attendance"){
            var p = {};
            p["name"] = data.pupil.name;
            p["userId"] = data.pupil.userId;
            s["pupils"].push(p);
          }
        }
        
      }
      o["stops"].push(s);
    }

    var stringJSON = JSON.stringify(o, null, "\t");
    return stringJSON;
  }

  private exportToCsv() {
    var firstStop: boolean = true;
    var firstPupil: boolean = true;
    var csvContent;
    var stringStops;
    var stringPupils;

    //create the first row + line, date and direction of the second
    csvContent = 'line' + ',' + 'date' + ',' + 'direction' + ',' + 'stops_time' + ',' + 'stops_name' + ',' + 'stops_pupils_name' + ',' + 'stops_pupils_uderId' + '\n'
    + '"' + this.selectedLine.name + '"' + ',' + '"' +  this.selectedRide.date + '"' + ',' + '"' + this.selectedRide.direction + '"';
    //concatenate stops
    for(let stop of (this.selectedRide.direction == 'O' ? this.stops.outwardStops : this.stops.returnStops)){
        if(firstStop){
          stringStops = ',' + '"' + stop.time + '"' + ',' + '"' + stop.name + '"';
          csvContent += stringStops;
          firstStop = false;
          //add users to the stop
          var stopPupils = this.rideData.get(stop.id);
          if(stopPupils != undefined){
            for(let data of stopPupils){
              if(data.type == "both" || data.type == "attendance"){
                if(firstPupil){
                  stringPupils = ',' + '"' + data.pupil.name + '"' + ',' + '"' + data.pupil.userId + '"' + '\n';
                  csvContent += stringPupils;
                  firstPupil = false;
                }else{
                  stringPupils = '""' + ',' + '""' + ',' + '""' + ',' + '""' + ',' + '""' + ',' + '"' + data.pupil.name + '"' + ',' + '"' + data.pupil.userId + '"' + '\n';
                  csvContent += stringPupils; 
                }
              }else{
                stringPupils = ',' + '""' + ',' + '""' + '\n';
                csvContent += stringPupils;
              }
            }
          }else{
            stringPupils = ',' + '""' + ',' + '""' + '\n';
            csvContent += stringPupils;
          }
          firstPupil = true;
          //
        }else{
          stringStops = '""' + ',' + '""' + ',' + '""' + ',' + '"' + stop.time + '"' + ',' + '"' + stop.name + '"';
          csvContent += stringStops;
          //add uders to the stop
          var stopPupils = this.rideData.get(stop.id);
          if(stopPupils != undefined && stopPupils.length != 0){
            for(let data of stopPupils){
              if(data.type == "both" || data.type == "attendance"){
                if(firstPupil){
                  stringPupils = + ',' + '"' + data.pupil.name + '"' + ',' + '"' + data.pupil.userId + '"' + '\n';
                  csvContent += stringPupils;
                  firstPupil = false;
                }else{
                  stringPupils = '""' + ',' + '""' + ',' + '""' + ',' + '""' + ',' + '""' + ',' + '"' + data.pupil.name + '"' + ',' + '"' + data.pupil.userId + '"' + '\n';
                  csvContent += stringPupils; 
                }
              }else{
                stringPupils = ',' + '""' + ',' + '""' + '\n';
                csvContent += stringPupils;
              }
            }
          }else{
            stringPupils = ',' + '""' + ',' + '""' + '\n';
            csvContent += stringPupils;
          }
          firstPupil = true;
          //
        }
    }

    return csvContent;
  }
}