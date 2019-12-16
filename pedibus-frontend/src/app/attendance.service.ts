import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs/index';
import { map } from 'rxjs/operators';

import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AttendanceService {

  constructor(private http: HttpClient) { }

  getRides(username: string, lineId: number) {
    return this.http.get(environment.apiUrl+'/users/'+username+'/rides?lineId='+lineId);
  }

  /**
   * Get attendances and reservations for the given ride and returns a data structure that combines
   * the information
   * @param rideId
   */
  getRideData(rideId: number) {
    let attendances$ = this.http.get(environment.apiUrl+'/rides/'+rideId+'/attendances');
    let reservations$ = this.http.get(environment.apiUrl+'/rides/'+rideId+'/reservations');

    return forkJoin([attendances$, reservations$]).pipe(
      map(([attendances, reservations]: [any[], any[]]) => {
        let dataMap = new Map();

        // Scan all attendances and add them
        for (let attendance of attendances) {
          if (!dataMap.has(attendance.stopId)) {
            dataMap.set(attendance.stopId, []);
          }

          let data = {
            type: attendance.hasReservation ? 'both' : 'attendance',
            attendanceId: attendance.id,
            pupil: attendance.pupil,
            stopId: attendance.stopId
          }

          dataMap.get(attendance.stopId).push(data);
        }

        // Scan all reservations and add only the ones without attendance
        for (let reservation of reservations) {
          if (!reservation.hasAttendance) {
            if (!dataMap.has(reservation.stopId)) {
              dataMap.set(reservation.stopId, []);
            }

            let data = {
              type: 'reservation',
              pupil: reservation.pupil,
              stopId: reservation.stopId
            }

            dataMap.get(reservation.stopId).push(data);
          }
        }

        return dataMap;
      })
    )
  }

  createAttendance(pupilId: number, rideId: number, stopId) {
    return this.http.post(environment.apiUrl+'/attendances', {pupilId, rideId, stopId});
  }

  deleteAttendance(attendanceId: number) {
    return this.http.delete(environment.apiUrl+'/attendances/'+attendanceId);
  }

}