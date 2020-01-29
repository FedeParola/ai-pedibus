import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthenticationService } from './authentication.service';
import { RxStompService } from '@stomp/ng2-stompjs';
import { Location } from '@angular/common';
import { NotificationService } from './notification.service';


@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  loggedIn: boolean;
  usersDisabled: boolean;
  ridesDisabled: boolean;
  selectedView;
  pendingNotifications: number;
  loggedUser: string;

  constructor(private authService: AuthenticationService,
              private router: Router,
              private rxStompService: RxStompService,
              private notificationService: NotificationService,
              private location: Location) {}

  ngOnInit() {
    this.notificationService.getNotificationsUpdate$().subscribe(
      (pendingNotifications: number) => {
        this.pendingNotifications = pendingNotifications;
      }
    );

    if(this.location.path() == '' && this.authService.isLoggedIn()){
      this.router.navigateByUrl('/reservation');
    }
    
    this.authService.getLoggedIn$().subscribe(
      (loggedIn) => {
        this.loggedIn = loggedIn;

        if (loggedIn) {
          console.log('Logged In');
          this.loggedUser = this.authService.getUsername();

          const roles = this.authService.getRoles();
          if (roles.indexOf('ROLE_ADMIN') > -1 || roles.indexOf('ROLE_SYSTEM-ADMIN') > -1) {
            this.usersDisabled = false;
            this.ridesDisabled = false;
          } else {
            this.usersDisabled = true;
            this.ridesDisabled = true;
          }

          this.rxStompService.activate();

        } else {
          console.log('Logged Out');
          
          this.router.navigateByUrl('/login');

          this.rxStompService.deactivate();
        }
      }
    )
  }

  logout() {
    this.authService.logout();
  }
}
