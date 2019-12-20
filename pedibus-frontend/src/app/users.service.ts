import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environments/environment'
import { AuthenticationService } from './authentication.service';
import { RxStompService } from '@stomp/ng2-stompjs';
import { concat } from 'rxjs';
import { map, concatAll } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class UsersService {

  constructor(private authenticationService: AuthenticationService,
              private http: HttpClient,
              private rxStompService: RxStompService) { }

  getUsers(page: number, size: number) {
    //return this.http.get(environment.apiUrl+'/users?page='+page+'&size='+size);
    let path = '/users?page=' + page + '&size=' + size;
    let url = environment.apiUrl + path;

    return concat(
      // Retrieve data for the first time
      this.http.get(url),

      // On every event retrieve data again
      this.rxStompService.watch("/topic/users").pipe(
        map(() => this.http.get(url)),
        concatAll()
      )
    );
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

  getPupils(username) {
    return this.http.get(environment.apiUrl+'/users/'+username+'/pupils');
  }
  
  getUserPupils(page: number, size: number){
    //return this.http.get(environment.apiUrl+'/users/'+this.authenticationService.getUsername()+'/pupils?page='+page+'&size='+size);
    let path = '/users/'+this.authenticationService.getUsername()+'/pupils?page='+page+'&size='+size;
    let url = environment.apiUrl + path;

    return concat(
      // Retrieve data for the first time
      this.http.get(url),

      // On every event retrieve data again
      this.rxStompService.watch("/topic/users/"+this.authenticationService.getUsername()+'/pupils').pipe(
        map(() => this.http.get(url)),
        concatAll()
      )
    );
  
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
