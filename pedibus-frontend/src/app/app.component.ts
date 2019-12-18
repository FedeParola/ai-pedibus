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
  usersDisabled: boolean;
  ridesDisabled: boolean;
  menuVisible: boolean;
  selectedView;

  constructor(private authService: AuthenticationService,
              private router: Router) {}

  ngOnInit() {
    if(this.authService.isLoggedOut()){
      this.logoutDisabled = true;
      this.menuVisible = false;
      this.router.navigateByUrl('/login');
    }else{
      const roles = this.authService.getRoles();
      if(roles.indexOf('ROLE_ADMIN') > -1){
        this.usersDisabled = false;
        this.ridesDisabled = false;
        this.menuVisible = true;
      }else{
        this.usersDisabled = true;
        this.ridesDisabled = true;
        this.menuVisible = true;
      }
    }
  }

  logout() {
    this.authService.logout();
    this.router.navigateByUrl('/login');
    this.logoutDisabled = true;
    this.usersDisabled = false;
    this.ridesDisabled = false;
    this.menuVisible = false;
  }

  users(){
    this.router.navigateByUrl('/users');
  }

  pupils(){
    this.router.navigateByUrl('/pupils');
  }

  notifications(){
    this.router.navigateByUrl('/notifications');
  }

  attendances(){
    this.router.navigateByUrl('/attendance');
  }

  availabilities(){
    this.router.navigateByUrl('/availability');
  }

  reservations(){
    this.router.navigateByUrl('/reservation');
  }

  rides(){
    this.router.navigateByUrl('/rides');
  }
}
