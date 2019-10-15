import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthenticationService } from './authentication.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit{
  logoutDisabled: boolean;

  constructor(private authService: AuthenticationService,
    private router: Router) {}

  ngOnInit() {
    if(this.authService.isLoggedOut()){
      this.logoutDisabled = true;
      this.router.navigateByUrl('/login');
    }
  }

  logout() {
    this.authService.logout();
    this.router.navigateByUrl('/login');
    this.logoutDisabled = true;
  }

  users(){
    this.router.navigateByUrl('/users');
  }

  attendances(){
    this.router.navigateByUrl('/attendance');
  }

}
