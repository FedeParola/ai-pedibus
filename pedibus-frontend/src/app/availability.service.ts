import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from 'src/environments/environment';
import { map, concatAll } from 'rxjs/operators';
import { Observable, concat } from 'rxjs';
import { RxStompService } from '@stomp/ng2-stompjs';

@Injectable({
  providedIn: 'root'
})
export class AvailabilityService {

  constructor(private http: HttpClient,
              private rxStompService: RxStompService) { }

  /**
   * Returns an observable the emits, for the given line, a map containing, for each ride,
   * the (possible) availability of the given user.
   * The observable emits the first time on subscription and every time there is a change in the data.
   * @param username 
   * @param lineId 
   */
  getAvailabilities(username: string, lineId: number): Observable<Map<number, any>> {
    let url = environment.apiUrl + '/users/'+username+'/availabilities?consolidatedOnly=false&lineId='+lineId;

    return concat(
      // Retrieve data for the first time
      this.http.get(url),

      // On every event retrieve data again
      this.rxStompService.watch('/topic/users/'+username+'/availabilities?lineId='+lineId).pipe(
        map(() => this.http.get(url)),
        concatAll()
      )
    ).pipe(
      map((res: any[]) => {
        let availabilitiesMap = new Map();

        for (let availability of res) {
          availabilitiesMap.set(availability.ride.id, availability);
        }

        return availabilitiesMap;
      })
    )
  }

  createAvailability(username: string, rideId: number, stopId: number) {
    return this.http.post(environment.apiUrl+'/availabilities', {'email': username, rideId, stopId});
  }

  updateAvailabilityStop(availabilityId: number, stopId: number) {
    return this.http.put(environment.apiUrl+'/availabilities/'+availabilityId, {stopId});
  }

  confirmAvailability(availabilityId: number) {
    return this.http.put(environment.apiUrl+'/availabilities/'+availabilityId, {status: 'CONFIRMED'});
  }

  deleteAvailability(availabilityId: number) {
    return this.http.delete(environment.apiUrl+'/availabilities/'+availabilityId);
  }
}
