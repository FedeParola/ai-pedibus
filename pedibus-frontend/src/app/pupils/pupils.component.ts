import { Component, OnInit, Inject, OnDestroy } from '@angular/core';
import { UsersService } from '../users.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material';
import { AuthenticationService } from '../authentication.service';
import {MatDialog, MatDialogRef, MatDialogConfig} from '@angular/material/dialog';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import { DialogPupilComponent } from './dialog-pupil/dialog-pupil.component';
import { RxStompService } from '@stomp/ng2-stompjs';

@Component({
  selector: 'app-pupils',
  templateUrl: './pupils.component.html',
  styleUrls: ['./pupils.component.css']
})
export class PupilsComponent implements OnInit, OnDestroy {
  pupils;
  pageNumber: number;
  pageSize: number;
  nextEnabled;
  prevEnabled;
  noPupils;
  pupilsSub;

  constructor(private usersService: UsersService,
              private authService: AuthenticationService,
              private router: Router,
              public dialog: MatDialog,
              private rxStompService: RxStompService,
              private _snackBar: MatSnackBar,
              private authenticationService: AuthenticationService) { 
                this.pageNumber = 0;
                this.pageSize = 6;
                this.noPupils = false;
              }

  ngOnInit() {
    
    this.pupilsSub=this.usersService.getUserPupils(this.pageNumber, this.pageSize).subscribe((res) => {
      this.pupils = res;
      this.prevEnabled = false;
      if(this.pupils.length==0){
        this.noPupils = true;
      }
      if(this.pupils[this.pupils.length - 1].hasNext){
        this.nextEnabled = true;
      }else{
        this.nextEnabled = false;
      }
    },
    (error) => {
      this.handleError(error)
    });
    
  }

  ngOnDestroy(): void {
    // Unsubscribe from all events
    if (this.pupilsSub) {
      this.pupilsSub.unsubscribe();
    }
  }

  nextPage(){
    this.pageNumber++;
    this.pupilsSub.unsubscribe();
    this.pupilsSub=this.usersService.getUserPupils(this.pageNumber, this.pageSize).subscribe((res) => {
      this.pupils = res;
      this.prevEnabled = true;
      if(this.pupils[this.pupils.length - 1].hasNext){
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
    this.pupilsSub.unsubscribe();
    this.pupilsSub=this.usersService.getUserPupils(this.pageNumber, this.pageSize).subscribe((res) => {
      this.pupils = res;
      if(this.pageNumber == 0){
        this.prevEnabled = false;
      }
      this.nextEnabled = true;
    },
    (error) => {
      this.handleError(error)
    });
  }

  openDialog(pupil): void {
    var command:string;
    const dialogConfig = new MatDialogConfig();
    dialogConfig.disableClose = true;
    dialogConfig.autoFocus = true;
    dialogConfig.width = '230px';

    if(pupil != null){
      command = 'edit';
    }else{
      command = 'add';
    }

    dialogConfig.data = {
      command: command,
      pupil: pupil
    };

    const dialogRef = this.dialog.open(DialogPupilComponent, dialogConfig);

    dialogRef.afterClosed().subscribe(result => {
      this.pupilsSub.unsubscribe();
      this.pupilsSub=this.usersService.getUserPupils(this.pageNumber, this.pageSize).subscribe((res) => {
        this.pupils = res;
        if(this.pageNumber > 0){
          this.prevEnabled = true;
        }else{
          this.prevEnabled = false;
        }
        if(this.pupils[this.pupils.length - 1].hasNext){
          this.nextEnabled = true;
        }else{
          this.nextEnabled = false;
        }
      },
      (error) => {
        this.handleError(error)
      });
    });
  }

  removePupil(pupil){
    const dialogConfig = new MatDialogConfig();
    dialogConfig.disableClose = true;
    dialogConfig.autoFocus = true;
    dialogConfig.width = '220px';

    dialogConfig.data = {
      pupil: pupil
    };

    const dialogRef = this.dialog.open(DialogRemovePupilComponent, dialogConfig);

    dialogRef.afterClosed().subscribe(result => {
      if(result.removed){
        const index = this.pupils.indexOf(pupil, 0);
        if (index > -1) {
          this.pupils.splice(index, 1);
        }
        //se la dimensione scende a 5 elementi
        if(this.pupils.length==5){
          this.ngOnInit();
        }else if(this.pupils.length==0){
          if(this.pageNumber>0){
            this.pageNumber--;
            this.ngOnInit();
          }else{
            this.ngOnInit();
          }
        }
      }
    });
  }

  private handleError(error: HttpErrorResponse) {
    if (!(error.error instanceof ErrorEvent) && error.status == 401) {
      /* Not authenticated or auth expired */
      this.authenticationService.logout();
    
    } else {
      /* All other errors*/
      console.error("Error contacting server");
      this._snackBar.open("Error in the communication with the server!", "", { panelClass: 'error-snackbar', duration: 5000 });
    }
  };

}

@Component({
  selector: 'dialog-remove-pupil',
  templateUrl: 'dialog-remove-pupil.component.html',
})
export class DialogRemovePupilComponent {
  currentPupil;

  constructor(private usersService: UsersService,
              public dialogRef: MatDialogRef<DialogRemovePupilComponent>,
              private authService: AuthenticationService,
              private _snackBar: MatSnackBar, @Inject(MAT_DIALOG_DATA) data) {

      this.currentPupil = data.pupil;
    }

    onCancelClick(): void {
      this.dialogRef.close({removed:false});
    }

    onAcceptClick(): void {
      this.usersService.removeUserPupil(this.currentPupil.id).subscribe((res) => {
        this._snackBar.open("Pupil removed", "",
                      { panelClass: 'success-snackbar', duration: 5000 });
        this.dialogRef.close({removed:true});
      },
      (error) => {
        this._snackBar.open("Cannot remove yuor pupil!", "",
                      { panelClass: 'error-snackbar', duration: 7000 });
      });
    }
  
}
