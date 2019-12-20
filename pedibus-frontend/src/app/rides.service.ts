import { Injectable } from '@angular/core';
import { environment } from '../environments/environment';
import { HttpClient } from '@angular/common/http';
import { map, concatAll } from 'rxjs/operators';
import { concat, Observable } from 'rxjs';
import { RxStompService } from '@stomp/ng2-stompjs';

@Injectable({
  providedIn: 'root'
})
export class RidesService {

  constructor(private http: HttpClient,
              private rxStompService: RxStompService) { }

  getMyLines(username: string){
    return this.http.get(environment.apiUrl+'/users/'+username+'/lines');
  }

  getRide(lineId: number, date: string, direction: string){
    let path = '/lines/'+lineId+'/rides?date='+date+'&direction='+direction;
    let url = environment.apiUrl + path;

    return concat(
      // Retrieve data for the first time
      this.http.get(url),
      
      // On every event retrieve data again
      this.rxStompService.watch("/topic" + path).pipe(
        map(() => this.http.get(url)),
        concatAll()
      )
    );
  }

  getRideAvailabilities(rideId: number) {
    let path = '/rides/'+rideId+'/availabilities';
    let url = environment.apiUrl + path;

    return concat(
      // Retrieve data for the first time
      this.retrieveAvailabilities(url),

      this.rxStompService.watch("/topic" + path).pipe(
        map(() =>  this.retrieveAvailabilities(url)),
        concatAll()
      )
    );
  }

  private retrieveAvailabilities(url: string){
    return this.http.get(url).pipe(
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
    )
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
