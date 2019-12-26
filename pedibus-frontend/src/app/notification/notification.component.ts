import { Component, OnInit, Inject  } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material';
import { AuthenticationService } from '../authentication.service';
import { MatDialog, MatDialogRef, MatDialogConfig } from '@angular/material/dialog';
import { MAT_DIALOG_DATA } from '@angular/material/dialog'

import { NotificationService } from '../notification.service';


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
        if(this.notifications[this.notifications.length - 1].hasNext){
          this.nextEnabled = true;
        }else{
          this.nextEnabled = false;
        }
      },
      (error) => {
        this.handleError(error)
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
        this.handleError(error)
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
        this.handleError(error)
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

  openDialogShowNotification(notification): void {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.disableClose = true;
    dialogConfig.autoFocus = true;
    dialogConfig.width = '230px';

    dialogConfig.data = {
      notification: notification
    };

    const dialogRef = this.dialog.open(DialogShowNotificationComponent, dialogConfig);

    dialogRef.afterClosed().subscribe(result => {
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
    });
  }

  private handleError(error: HttpErrorResponse) {
    if (!(error.error instanceof ErrorEvent) && error.status == 401) {
      /* Not authenticated or auth expired */
      this.authService.logout();
    
    } else {
      /* All other errors*/
      console.error("Error contacting server");
      this._snackBar.open("Error in the communication with the server!", "", { panelClass: 'error-snackbar', duration: 5000 });
    }
  };

}


@Component({
  selector: 'dialog-show-notification',
  templateUrl: 'dialog-show-notification.component.html',
})
export class DialogShowNotificationComponent {
  currentNotification;

  constructor(
    public dialogRef: MatDialogRef<DialogShowNotificationComponent>,
    private authService: AuthenticationService,
    private _snackBar: MatSnackBar, @Inject(MAT_DIALOG_DATA) data) {

      this.currentNotification = data.notification;
    }

    onDoneClick(): void {
      this.dialogRef.close();
    }
  
}
