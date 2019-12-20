import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from 'src/environments/environment';
import { map, concatAll } from 'rxjs/operators';
import { Observable, concat } from 'rxjs';
import { RxStompService } from '@stomp/ng2-stompjs';

@Injectable({
  providedIn: 'root'
})
export class ReservationService {
  constructor(private http: HttpClient,
              private rxStompService: RxStompService) { }

  /**
   * Returns an observable that emits the list of reservations of the given pupil.
   * The observable emits the first time on subscription and every time there is a change
   * in the data.
   * @param lineId 
   */
  getReservations(pupilId: number): Observable<Map<number, any>> {
    let path = '/pupils/' + pupilId + '/reservations';
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
        let reservationsMap = new Map();

        for (let reservation of res) {
          reservationsMap.set(reservation.rideId, reservation);
        }

        return reservationsMap;
      })
    );
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
