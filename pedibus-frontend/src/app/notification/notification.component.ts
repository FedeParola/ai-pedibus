import { Component, OnInit, Inject  } from '@angular/core';
import { MatSnackBar } from '@angular/material';
import { MatDialog, MatDialogRef, MatDialogConfig } from '@angular/material/dialog';
import { MAT_DIALOG_DATA } from '@angular/material/dialog'

import { AuthenticationService } from '../authentication.service';
import { NotificationService } from '../notification.service';
import { handleError } from '../utils';


@Component({
  selector: 'app-notification',
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.css']
})
export class NotificationComponent implements OnInit {
  notifications;
  pageNumber: number;
  pageSize: number;
  nextEnabled;
  prevEnabled;
  noNotifications;
  notificationsSub = undefined;

  constructor(private notificationService: NotificationService,
              private authService: AuthenticationService,
              public dialog: MatDialog,
              private _snackBar: MatSnackBar) { 
                this.pageNumber = 0;
                this.pageSize = 6;
                this.noNotifications = false;
    }

  ngOnInit() {
    this.updateNotifications();
    if(this.notificationsSub == undefined) {
      this.notificationsSub = this.notificationService.getNotificationsUpdate$().subscribe(
        (res) => {
          this.updateNotifications()
        }
      );
    }
  }

  ngOnDestroy() {
    this.notificationsSub.unsubscribe();
  }

  updateNotifications(){
    this.notificationService.getNotifications(this.authService.getUsername(),
                                              this.pageNumber,
                                              this.pageSize).subscribe(
      (res) => {
        this.notifications = res;
        if(this.notifications.length==0){
          this.noNotifications = true;
        }
        if(this.pageNumber == 0){
          this.prevEnabled = false;
        }
        if(this.notifications.length > 0 && this.notifications[this.notifications.length - 1].hasNext){
          this.nextEnabled = true;
        }else{
          this.nextEnabled = false;
        }
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  nextPage(){
    this.pageNumber++;
    this.notificationService.getNotifications(this.authService.getUsername(),
                                              this.pageNumber,
                                              this.pageSize).subscribe(
      (res) => {
        this.notifications = res;
        this.prevEnabled = true;
        if(this.notifications[this.notifications.length - 1].hasNext){
          this.nextEnabled = true;
        }else{
          this.nextEnabled = false;
        }
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  prevPage(){
    this.pageNumber--;
    this.notificationService.getNotifications(this.authService.getUsername(),
                                              this.pageNumber,
                                              this.pageSize).subscribe(
      (res) => {
        this.notifications = res;
        if(this.pageNumber == 0){
          this.prevEnabled = false;
        }
        this.nextEnabled = true;
      },
      (error) => {
        handleError(error, this._snackBar);
      }
    );
  }

  removeNotification(notification){
    this.notificationService.removeNotification(notification.id).subscribe((res) => {
      //dovrebbe funzionare con questo al posto della remove
      const index = this.notifications.indexOf(notification, 0);
      if (index > -1) {
        this.notifications.splice(index, 1);
      }
      //se la dimensione scende a 5 elementi
      if(this.notifications.length==5){
        this.ngOnInit();
      }else if(this.notifications.length==0){
        if(this.pageNumber>0){
          this.pageNumber--;
          this.ngOnInit();
        }else{
          this.ngOnInit();
        }
      }
      this._snackBar.open("Notification removed", "",
                    { panelClass: 'success-snackbar', duration: 5000 });
    },
    (error) => {
      this._snackBar.open("Cannot remove yuor notification!", "",
                    { panelClass: 'error-snackbar', duration: 7000 });
    });
  }

  readNotification(notification){
    if(!notification.read){
      this.notificationService.updateNotification(notification.id, true)
      .subscribe(
        () => {
          notification.read = true;
          console.log("Notification read");
        },
        () => {
          this._snackBar.open("Error in the communication with the server!", "",
              { panelClass: 'error-snackbar', duration: 5000 });
        }
      );
    }
  }
}