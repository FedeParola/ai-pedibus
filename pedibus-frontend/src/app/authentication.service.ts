import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { shareReplay, tap } from 'rxjs/operators';
import { decode } from 'jsonwebtoken';
import { RxStompService } from '@stomp/ng2-stompjs';

import { environment } from '../environments/environment'
import { BehaviorSubject } from 'rxjs';


@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {
  private loggedIn$: BehaviorSubject<boolean>;

  constructor(private http: HttpClient,
              private rxStompService: RxStompService) {
    this.loggedIn$ = new BehaviorSubject(this.isLoggedIn());

    let token = localStorage.getItem('id_token');
    if (token) {
      this.rxStompService.configure({
        connectHeaders: {
          login: token
        }
      });
    }

  }

  /**
   * Returns an observable that emits the logged in status of the client (true or false),
   * the first time on subscription and every time the user logs in or out.
   */
  getLoggedIn$() {
    return this.loggedIn$.asObservable();
  }

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
    this.rxStompService.configure({
      connectHeaders: {
        login: authResult.token
      }
    });
    this.loggedIn$.next(true);
  }          

  logout() {
      localStorage.removeItem("id_token");
      localStorage.removeItem("expires_at");
      localStorage.removeItem("username");
      localStorage.removeItem("roles");
      this.loggedIn$.next(false);
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
