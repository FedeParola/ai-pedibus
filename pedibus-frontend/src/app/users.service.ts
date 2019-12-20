import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RxStompService } from '@stomp/ng2-stompjs';
import { concat } from 'rxjs';

import { environment } from '../environments/environment'
import { AuthenticationService } from './authentication.service';
import { map, concatAll } from 'rxjs/operators';


@Injectable({
  providedIn: 'root'
})
export class UsersService {

  constructor(private authenticationService: AuthenticationService,
              private http: HttpClient,
              private rxStompService: RxStompService) { }

  getUsers(page: number, size: number) {
    return this.http.get(environment.apiUrl+'/users?page='+page+'&size='+size);
  }

  getCurrentUserLines(){
      return this.http.get(environment.apiUrl+'/users/'+this.authenticationService.getUsername()+'/lines');
  }

  removeUserLine(username, lineId){
      return this.http.delete(environment.apiUrl+'/users/'+username+'/lines/'+lineId);
  }

  addUserLine(username, lineId){
      return this.http.post(environment.apiUrl+'/users/'+username+'/lines', [lineId]);
  }

  /**
   * Returns an observable that emits the list of pupils associated to the given user.
   * The observable emits the first time on subscription and every time there is a change
   * in the data.
   * @param username
   */
  getPupils(username) {
    let path = '/users/' + username + '/pupils';
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
  
  getUserPupils(page: number, size: number){
    return this.http.get(environment.apiUrl+'/users/'+this.authenticationService.getUsername()+'/pupils?page='+page+'&size='+size);
  }

  removeUserPupil(pupilId){
      return this.http.delete(environment.apiUrl+'/pupils/'+pupilId);
  }

  addUserPupil(name: string, userId, lineId){
      return this.http.post(environment.apiUrl+'/pupils', {name, userId, lineId});
  }

  updateUserPupil(name:string, lineId, pupilId){
      return this.http.put(environment.apiUrl+'/pupils/'+pupilId, {name, lineId});
  }
}
