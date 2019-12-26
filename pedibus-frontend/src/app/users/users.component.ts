import { Component, OnInit, OnDestroy } from '@angular/core';
import { MatDialog, MatDialogRef, MatDialogConfig } from '@angular/material/dialog';
import { DialogUserLinesComponent } from './dialog-user-lines/dialog-user-lines.component';
import { UsersService } from '../users.service';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material';
import { FormGroup, FormBuilder, Validators} from '@angular/forms';
import { AuthenticationService } from '../authentication.service';

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css']
})
export class UsersComponent implements OnInit, OnDestroy {
  users;
  pageNumber: number;
  pageSize: number;
  nextEnabled;
  prevEnabled;
  usersSub;

  constructor(private usersService: UsersService,
              private authService: AuthenticationService,
              public dialog: MatDialog,
              private _snackBar: MatSnackBar) { 
                this.pageNumber = 0;
                this.pageSize = 6;
              }

  ngOnInit() {
    this.usersSub=this.usersService.getUsers(this.pageNumber, this.pageSize).subscribe((res) => {
      this.users = res;
      this.prevEnabled = false;
      if(this.users[this.users.length - 1].hasNext){
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
    if (this.usersSub) {
      this.usersSub.unsubscribe();
    }
  }

  checkCurrent(username: string): Boolean{
    if(username == this.authService.getUsername()){
      return true;
    }
    return false;
  }

  checkSystemAdmin(user): Boolean{
    const index = this.users.indexOf(user);
    if(this.users[index].roles.indexOf("ROLE_SYSTEM-ADMIN") > -1){
      return true;
    }
    return false;
  }

  nextPage(){
    this.pageNumber++;
    this.usersSub.unsubscribe();
    this.usersSub=this.usersService.getUsers(this.pageNumber, this.pageSize).subscribe((res) => {
      this.users = res;
      this.prevEnabled = true;
      if(this.users[this.users.length - 1].hasNext){
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
    this.usersSub.unsubscribe();
    this.usersSub=this.usersService.getUsers(this.pageNumber, this.pageSize).subscribe((res) => {
      this.users = res;
      if(this.pageNumber == 0){
        this.prevEnabled = false;
      }
      this.nextEnabled = true;
    },
    (error) => {
      this.handleError(error)
    });
  }

  openDialogNewUser(): void {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.disableClose = true;
    dialogConfig.autoFocus = true;
    dialogConfig.width = '230px';

    const dialogRef = this.dialog.open(DialogNewUserComponent, dialogConfig);

    dialogRef.afterClosed().subscribe(result => {
      console.log('The dialog was closed');
    });

    dialogRef.afterClosed().subscribe(result => {
      this.usersSub.unsubscribe();
      this.usersSub=this.usersService.getUsers(this.pageNumber, this.pageSize).subscribe((res) => {
        this.users = res;
        if(this.pageNumber > 0){
          this.prevEnabled = true;
        }else{
          this.prevEnabled = false;
        }
        if(this.users[this.users.length - 1].hasNext){
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

  openDialogUserLines(username, lines): void {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.disableClose = true;
    dialogConfig.autoFocus = true;
    dialogConfig.width = '230px';
    dialogConfig.data = {
      username: username,
      userLines: lines
    };

    const dialogRef = this.dialog.open(DialogUserLinesComponent, dialogConfig);

    dialogRef.afterClosed().subscribe(result => {
      console.log('The dialog was closed');
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
  selector: 'dialog-new-user',
  templateUrl: 'dialog-new-user.component.html',
})
export class DialogNewUserComponent {
  form: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<DialogNewUserComponent>,
    private fb: FormBuilder, 
    private authService: AuthenticationService,
    private _snackBar: MatSnackBar) {
      this.form = this.fb.group({
        email: ['', [Validators.required, Validators.email]]
      });
    }

  onCancelClick(): void {
    this.dialogRef.close();
  }

  onAddClick(): void {
    const val = this.form.value;

    if (this.form.valid) {
      this.authService.register(val.email)
          .subscribe(
              () => {
                console.log("User is registered");
                this.dialogRef.close();
                this._snackBar.open("Account created", "",
                    { panelClass: 'success-snackbar', duration: 5000 });
              },
              () => {
                this._snackBar.open("Error in the communication with the server!", "",
                    { panelClass: 'error-snackbar', duration: 5000 });
              }
          );
    }
  }

  checkEmail() {
    const val = this.form.value;
    if(val.email){
      this.authService.checkEmail(val.email).
        subscribe(
          () => {
            return this.form.controls['email'].setErrors({ alreadyUsed: true });
          },
          () => {
            return null;
          }
        )
    }
  }

}
