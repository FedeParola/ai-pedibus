import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { shareReplay, tap } from 'rxjs/operators';
import { decode } from 'jsonwebtoken';
import { environment } from '../environments/environment'

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {

  constructor(private http: HttpClient) { }

  login(email:string, password:string ) {
    return this.http.post(environment.apiUrl+'/login', {email, password}).pipe(
      tap(res => this.setSession(res)),
      shareReplay()
    );
  }

  register(email:string) {
    return this.http.post(environment.apiUrl+'/users', {email});
  }
      
  private setSession(authResult) {
    let payload = decode(authResult.token);
    localStorage.setItem('id_token', authResult.token);
    localStorage.setItem('username', payload.sub);
    localStorage.setItem('roles', payload.roles);
    localStorage.setItem("expires_at", JSON.stringify(payload.exp));
  }          

  logout() {
      localStorage.removeItem("id_token");
      localStorage.removeItem("expires_at");
      localStorage.removeItem("username");
      localStorage.removeItem("roles");
  }

  public isLoggedIn() {
      return new Date() < this.getExpiration();
  }

  isLoggedOut() {
      return !this.isLoggedIn();
  }

  getRoles(){
    const roles = localStorage.getItem("roles").split(',');
    return roles;
  }

  getUsername() {
    return localStorage.getItem('username')
  }

  getExpiration() {
      const expiration = localStorage.getItem("expires_at");
      const expiresAt = JSON.parse(expiration);
      return new Date(expiresAt*1000);
  }

  checkEmail(username: string){
    return this.http.get(environment.apiUrl+'/users/'+username+'?check=true');
  }
}
