import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class LineService {

  constructor(private http: HttpClient) { }

  getLines(){
    return this.http.get(environment.apiUrl+'/lines');
  }

  getStops(lineId: number) {
    return this.http.get(environment.apiUrl+'/lines/'+lineId);
  }

  getPupils(lineId: number) {
    return this.http.get(environment.apiUrl+'/lines/'+lineId+'/pupils');
  }

  getRides(lineId: number) {
    return this.http.get(environment.apiUrl+'/lines/'+lineId+'/rides');
  }
}
