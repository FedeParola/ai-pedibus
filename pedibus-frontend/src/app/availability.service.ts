import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from 'src/environments/environment';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AvailabilityService {

  constructor(private http: HttpClient) { }

  getAvailabilities(username: string, lineId: number): Observable<Map<number, any>> {
    return this.http
      .get(environment.apiUrl+'/users/'+username+'/availabilities?consolidatedOnly=false&lineId='+lineId)
      .pipe(
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
