import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '../environments/environment';
import { RxStompService } from '@stomp/ng2-stompjs';
import { concat } from 'rxjs';
import { map, concatAll } from 'rxjs/operators';

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

  getPupils(lineId: number) {
    let path = '/lines/' + lineId + '/pupils';
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

  getRides(lineId: number) {
    return this.http.get(environment.apiUrl+'/lines/'+lineId+'/rides');
  }
}
