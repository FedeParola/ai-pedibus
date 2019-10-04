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

  register(email:string, pass:string, confPass: string) {
    return this.http.post('http://localhost:8080/register', {email, pass, confPass});
  }
      
  private setSession(authResult) {
    let payload = decode(authResult.token);
    localStorage.setItem('id_token', authResult.token);
    localStorage.setItem("expires_at", JSON.stringify(payload.exp));
  }          

  logout() {
      localStorage.removeItem("id_token");
      localStorage.removeItem("expires_at");
  }

  public isLoggedIn() {
      return new Date() < this.getExpiration();
  }

  isLoggedOut() {
      return !this.isLoggedIn();
  }

  getExpiration() {
      const expiration = localStorage.getItem("expires_at");
      const expiresAt = JSON.parse(expiration);
      return new Date(expiresAt*1000);
  }

  checkEmail(email: string){
    return this.http.get(environment.apiUrl+'/register/'+email);
  }
}
