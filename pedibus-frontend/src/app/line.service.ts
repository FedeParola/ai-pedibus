import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RxStompService } from '@stomp/ng2-stompjs';
import { concat } from 'rxjs';
import { map, concatAll } from 'rxjs/operators';

import { environment } from '../environments/environment';


@Injectable({
  providedIn: 'root'
})
export class LineService {

  constructor(private http: HttpClient,
              private rxStompService: RxStompService) { }

  getLines(){
    return this.http.get(environment.apiUrl+'/lines');
  }

  getStops(lineId: number) {
    return this.http.get(environment.apiUrl+'/lines/'+lineId);
  }

  /**
   * Returns an observable that emits the list of pupils associated to the given line.
   * The observable emits the first time on subscription and every time there is a change
   * in the data.
   * @param lineId 
   */
  getPupils(lineId: number) {
    let path = '/lines/' + lineId + '/pupils';
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
   * Returns an observable that emits the list of rides associated to the given line.
   * The observable emits the first time on subscription and every time there is a change
   * in the data.
   * @param lineId 
   */
  getRides(lineId: number) {
    let path = '/lines/' + lineId + '/rides';
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
}
