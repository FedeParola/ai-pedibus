import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { AuthenticationService } from './authentication.service';
import { RxStompService } from '@stomp/ng2-stompjs';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  logoutDisabled: boolean;
  usersDisabled: boolean;
  ridesDisabled: boolean;
  menuVisible: boolean;
  selectedView;

  constructor(private authService: AuthenticationService,
              private router: Router,
              private rxStompService: RxStompService) {}

  ngOnInit() {
    this.rxStompService.watch('/user/topic/notifications').subscribe(
      () => {
        console.log('New notifications available');

        // Add here code to handle new notifications
      }
    );
    
    this.authService.getLoggedIn$().subscribe(
      (loggedIn) => {
        if (loggedIn) {
          console.log('Logged In');

          this.logoutDisabled = false;
          this.menuVisible = true;

          const roles = this.authService.getRoles();
          if(roles.indexOf('ROLE_ADMIN') > -1){
            this.usersDisabled = false;
            this.ridesDisabled = false;
          }else{
            this.usersDisabled = true;
            this.ridesDisabled = true;
          }

          this.rxStompService.activate();

        } else {
          console.log('Logged Out');
          this.logoutDisabled = true;
          this.menuVisible = false;
          this.router.navigateByUrl('/login');

          this.rxStompService.deactivate();
        }
      }
    )
  }

  logout() {
    this.authService.logout();
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
