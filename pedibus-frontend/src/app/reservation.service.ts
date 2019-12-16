import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from 'src/environments/environment';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ReservationService {
  constructor(private http: HttpClient) { }

  getReservations(pupilId: number): Observable<Map<number, any>> {
    return this.http
      .get(environment.apiUrl+'/pupils/'+pupilId+'/reservations')
      .pipe(
        map((res: any[]) => {
          let reservationsMap = new Map();

          for (let reservation of res) {
            reservationsMap.set(reservation.rideId, reservation);
          }

          return reservationsMap;
        })
      )
  }

  deleteReservation(id: number) {
    return this.http.delete(environment.apiUrl+'/reservations/'+id);
  }

  createReservation(pupilId: number, rideId: number, stopId: number) {
    return this.http.post(environment.apiUrl+'/reservations', {pupilId, rideId, stopId});
  }

  updateReservationStop(id: number, stopId: number) {
    return this.http.put(environment.apiUrl+'/reservations/'+id, stopId);
  }
}
