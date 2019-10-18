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
}
