import { Component, OnInit, Inject } from '@angular/core';
import {MatDialogRef, MAT_DIALOG_DATA} from '@angular/material/dialog';
import { LineService } from '../../line.service';
import { Router } from '@angular/router';
import { MatSnackBar} from '@angular/material';
import { HttpErrorResponse } from '@angular/common/http';
import { PupilsComponent } from '../pupils.component';
import { UsersService } from '../../users.service';
import { AuthenticationService } from '../../authentication.service';
import { FormGroup, FormBuilder, Validators} from '@angular/forms';

export interface Lines {
  value: string;
  viewValue: string;
}

@Component({
  selector: 'app-dialog-pupil',
  templateUrl: './dialog-pupil.component.html',
  styleUrls: ['./dialog-pupil.component.css']
})
export class DialogPupilComponent implements OnInit {
  form: FormGroup;

  command;
  currentPupil;
  addPupil:boolean;
  lines;
  previousPupilName;
  inputValue;
  selected;

  constructor(private lineService: LineService,
              private usersService: UsersService,
              private authService: AuthenticationService,
              private router: Router,
              private _snackBar: MatSnackBar,
              private fb: FormBuilder, 
              public dialogRef: MatDialogRef<DialogPupilComponent>, @Inject(MAT_DIALOG_DATA) data) { 
    
    this.form = this.fb.group({
      pupilName: ['', [Validators.required, Validators.pattern('[A-Za-z]*')]]
    });

    this.command = data.command;
    if(this.command == 'add'){
      this.addPupil = true;
    }else{
      this.addPupil = false;
      this.currentPupil = data.pupil;
      this.previousPupilName = this.currentPupil.name;//sa the previous name of the pupil before changing
      this.inputValue = this.previousPupilName;
    }
  }

  ngOnInit() {
    this.lineService.getLines().subscribe((res) => {
      this.lines = res;
      if(this.command == 'add'){
        this.selected = res[0];
      }else{
        for (var l of this.lines) {
          if(l.name == this.currentPupil.lineName){
            this.selected = l;
          }
        }
      }
    },
    (error) => {
      this.handleError(error)
    });
  }
  
  onCancelClick(): void {
    this.dialogRef.close();
  }

  onAddClick(): void {
    const val = this.form.value;

    if (this.form.valid) {
      this.usersService.addUserPupil(val.pupilName,this.authService.getUsername(),this.selected.id)
      .subscribe(
        () => {
          console.log("Pupil added");
          this.dialogRef.close();
          this._snackBar.open("Pupil created", "",
              { panelClass: 'success-snackbar', duration: 5000 });
        },
        () => {
          this._snackBar.open("Error in the communication with the server!", "",
              { panelClass: 'error-snackbar', duration: 5000 });
        }
      );
    }
  }

  onEditClick(): void{
    const val = this.form.value;
    var name:string;

    if (this.form.valid) {
      if(this.previousPupilName == val.pupilName){//if the name of the pupil doesn't change I set the pupil name to null
        name = null;
      }else{
        name=val.pupilName
      }
      this.usersService.updateUserPupil(name,this.selected.id,this.currentPupil.id)
      .subscribe(
        () => {
          console.log("Pupil edited");
          this.dialogRef.close();
          this._snackBar.open("Pupil created", "",
              { panelClass: 'success-snackbar', duration: 5000 });
        },
        () => {
          this._snackBar.open("Error in the communication with the server!", "",
              { panelClass: 'error-snackbar', duration: 5000 });
        }
      );
    }
  }

  private handleError(error: HttpErrorResponse) {
    if (!(error.error instanceof ErrorEvent) && error.status == 401) {
      // Not authenticated or auth expired
      this.router.navigateByUrl('/login');
    
    } else {
      // All other errors
      console.error("Error contacting server");
      this._snackBar.open("Error in the communication with the server!", "", { panelClass: 'error-snackbar', duration: 5000 });
    }
  };

}
