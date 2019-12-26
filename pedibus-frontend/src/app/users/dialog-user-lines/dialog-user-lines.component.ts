import { Component, OnInit, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { UsersService } from '../../users.service';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material';
import { AuthenticationService } from '../../authentication.service';


export interface Lines {
  value: string;
  viewValue: string;
}

@Component({
  selector: 'app-dialog-user-lines',
  templateUrl: './dialog-user-lines.component.html',
  styleUrls: ['./dialog-user-lines.component.css']
})
export class DialogUserLinesComponent implements OnInit {
  selectedUserLines;
  currentUserLines;
  selectedUsername;

  selected;

  constructor(private authService: AuthenticationService,
              private usersService: UsersService,
              private _snackBar: MatSnackBar,
              public dialogRef: MatDialogRef<DialogUserLinesComponent>,
              @Inject(MAT_DIALOG_DATA) data) {
      this.selectedUsername = data.username;
      this.selectedUserLines = data.userLines;
    }

  ngOnInit() {
    this.usersService.getCurrentUserLines().subscribe((res) => {
      this.currentUserLines = res;
      this.selected = res[0];
    },
    (error) => {
      this.handleError(error)
    });
  }

  checkDisabled(line): Boolean{
    if(this.currentUserLines != undefined){
      for(let l of this.currentUserLines){
        if(l.name == line.name){
          return false;
        }
      }
    }
    return true;
  }

  removeLine(line){
    this.usersService.removeUserLine(this.selectedUsername, line.id).subscribe((res) => {
      //dovrebbe funzionare con questo al posto della remove
      const index = this.selectedUserLines.indexOf(line, 0);
      if (index > -1) {
        this.selectedUserLines.splice(index, 1);
      }
      //this.selectedUserLines.pop(line);
      this._snackBar.open("Line removed", "",
                    { panelClass: 'success-snackbar', duration: 5000 });
    },
    (error) => {
      this._snackBar.open("Error in the communication with the server!", "",
                    { panelClass: 'error-snackbar', duration: 5000 });
    });
  }

  addLine(){
    for(const i of this.selectedUserLines){
      if(i.id == this.selected.id){
        this._snackBar.open("Line already administrated!", "",
                    { panelClass: 'error-snackbar', duration: 5000 });
        return;
      }
    }
    this.usersService.addUserLine(this.selectedUsername, this.selected.id).subscribe((res) => {
      this.selectedUserLines.push(this.selected);
      this._snackBar.open("Line added", "",
                    { panelClass: 'success-snackbar', duration: 5000 });
    },
    (error) => {
      this._snackBar.open("Error in the communication with the server!", "",
                    { panelClass: 'error-snackbar', duration: 5000 });
    });
  }

  onDoneClick(): void {
    this.dialogRef.close();
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
