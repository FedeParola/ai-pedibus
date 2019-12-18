import { Component, OnInit, Inject  } from '@angular/core';
import { UsersService } from '../users.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material';
import { AuthenticationService } from '../authentication.service';
import {MatDialog, MatDialogRef, MatDialogConfig} from '@angular/material/dialog';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';

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

  constructor(private usersService: UsersService,
              private authService: AuthenticationService,
              public dialog: MatDialog,
              private router: Router,
              private _snackBar: MatSnackBar) { 
                this.pageNumber = 0;
                this.pageSize = 6;
                this.noNotifications = false;
    }

  ngOnInit() {
    this.usersService.getUserNotifications(this.pageNumber, this.pageSize).subscribe((res) => {
      this.notifications = res;
      this.prevEnabled = false;
      if(this.notifications.length==0){
        this.noNotifications = true;
      }
      if(this.notifications[this.notifications.length - 1].hasNext){
        this.nextEnabled = true;
      }else{
        this.nextEnabled = false;
      }
    },
    (error) => {
      this.handleError(error)
    });
  }

  nextPage(){
    this.pageNumber++;
    this.usersService.getUserNotifications(this.pageNumber, this.pageSize).subscribe((res) => {
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
    });
  }

  prevPage(){
    this.pageNumber--;
    this.usersService.getUserNotifications(this.pageNumber, this.pageSize).subscribe((res) => {
      this.notifications = res;
      if(this.pageNumber == 0){
        this.prevEnabled = false;
      }
      this.nextEnabled = true;
    },
    (error) => {
      this.handleError(error)
    });
  }

  removeNotification(notification){
    this.usersService.removeUserNotification(notification.id).subscribe((res) => {
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
      notification.read = true;
      //put on notifications endpoint to update the status 
    });
  }

  private handleError(error: HttpErrorResponse) {
    if (!(error.error instanceof ErrorEvent) && error.status == 401) {
      /* Not authenticated or auth expired */
      this.router.navigateByUrl('/login');
    
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
