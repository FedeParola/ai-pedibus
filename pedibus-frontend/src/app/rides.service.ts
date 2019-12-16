import { Injectable } from '@angular/core';
import { environment } from '../environments/environment';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class RidesService {

  constructor(private http: HttpClient) { }

  getMyLines(username: string){
    return this.http.get(environment.apiUrl+'/users/'+username+'/lines');
  }

  getRide(lineId: number, date: string, direction: string){
    return this.http.get(environment.apiUrl+'/lines/'+lineId+'/rides?date='+date+'&direction='+direction);
  }

  getRideAvailabilities(rideId: number) {
    return this.http.get(environment.apiUrl+'/rides/'+rideId+'/availabilities').pipe(
      map((availabilities: any[]) => {
        let dataMap = new Map();

        /* Scan all availabilities and add them */
        for (let availability of availabilities) {
          if (!dataMap.has(availability.stopId)) {
            dataMap.set(availability.stopId, []);
          }

          let data = {
            id: availability.id,
            stopId: availability.stopId,
            userId: availability.userId,
            status: availability.status
          }

          dataMap.get(availability.stopId).push(data);
        }

        return dataMap;
      })
    );
  }

  createRide(date: string, lineId: number, direction: string){
    return this.http.post(environment.apiUrl+'/rides', {date, lineId, direction});
  }

  changeRideStatus(rideId: number, consolidated: boolean){
    return this.http.put(environment.apiUrl+'/rides/'+rideId, {consolidated: consolidated});
  }

  deleteRide(rideId: number){
    return this.http.delete(environment.apiUrl+'/rides/'+rideId);
  }

  updateAvailability(availabilityId: number, newStatus: string){
    return this.http.put(environment.apiUrl+'/availabilities/'+availabilityId, {status: newStatus});
  }
}
