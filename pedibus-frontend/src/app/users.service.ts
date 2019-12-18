import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environments/environment'
import { AuthenticationService } from './authentication.service';

@Injectable({
  providedIn: 'root'
})
export class UsersService {

  constructor(private authenticationService: AuthenticationService,
              private http: HttpClient) { }

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

  getPupils(username) {
    return this.http.get(environment.apiUrl+'/users/'+username+'/pupils');
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

  getUserNotifications(page: number, size: number){
    return this.http.get(environment.apiUrl+'/users/'+this.authenticationService.getUsername()+'/notifications?page='+page+'&size='+size);
  }

  removeUserNotification(notificationId){
    return this.http.delete(environment.apiUrl+'/notifications/'+notificationId);
  }

  updateUserNotification(notificationId, read: boolean){
    return this.http.put(environment.apiUrl+'/notifications/'+notificationId, {read});
}

}
