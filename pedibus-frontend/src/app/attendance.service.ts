import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { concat, forkJoin, merge, Observable } from 'rxjs';
import { map, concatAll } from 'rxjs/operators';

import { environment } from '../environments/environment';
import { RxStompService } from '@stomp/ng2-stompjs';

@Injectable({
  providedIn: 'root'
})
export class AttendanceService {

  constructor(private http: HttpClient,
              private rxStompService: RxStompService) { }

  /**
   * Returns an observable the emits the list of rides of the given line for which the given user
   * is escort. The observable emits the first time on subscription and every time there is 
   * a change in the data.
   * @param username 
   * @param lineId 
   */
  getRides(username: string, lineId: number) {
    let path = '/users/'+username+'/rides?lineId='+lineId;
    let url = environment.apiUrl + path;

    return concat(
      // Retrieve data for the first time
      this.http.get(url),

      // On every event retrieve data again
      this.rxStompService.watch('/topic' + path).pipe(
        map(() => this.http.get(url)),
        concatAll()
      )
    );
  }

  /**
   * Returns an observable that emits a map containing, for each stop, data that combines
   * information of attendances and reservations for the given ride. 
   * The observable emits the first time on subscription and every time there is a change
   * in the data.
   * @param rideId
   */
  getRideData(rideId: number) {
    return concat(
      // Retrieve data for the first time
      this.doGetRideData(rideId),

      // On every event retrieve data again
      merge(
        this.rxStompService.watch('/topic/rides/' + rideId + '/attendances'),
        this.rxStompService.watch('/topic/rides/' + rideId + '/reservations'),
      ).pipe(
        map(() => this.doGetRideData(rideId)),
        concatAll()
      )
    );
  }

  doGetRideData(rideId: number) {
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

        // Sort pupils for each stop
        dataMap.forEach((data) => data.sort((a, b) => a.pupil.name.localeCompare(b.pupil.name)));

        return dataMap;
      })
    )
  }

  /**
   * Returns an observable that emits the list of attendances of the given pupil.
   * The observable emits the first time on subscription and every time there is a change
   * in the data.
   * @param pupilId 
   */
  getAttendancesByPupil(pupilId: number): Observable<Map<number, any>> {
    let path = '/pupils/' + pupilId + '/attendances';
    let url = environment.apiUrl + path;
    
    return concat(
      // Retrieve data for the first time
      this.http.get(url),

      // On every event retrieve data again
      this.rxStompService.watch('/topic' + path).pipe(
        map(() => this.http.get(url)),
        concatAll()
      )
    ).pipe(
      map((res: any[]) => {
        let attendancesMap = new Map();

        for (let attendace of res) {
          attendancesMap.set(attendace.rideId, attendace);
        }

        return attendancesMap;
      })
    );
  }

  createAttendance(pupilId: number, rideId: number, stopId) {
    return this.http.post(environment.apiUrl+'/attendances', {pupilId, rideId, stopId});
  }

  deleteAttendance(attendanceId: number) {
    return this.http.delete(environment.apiUrl+'/attendances/'+attendanceId);
  }

}