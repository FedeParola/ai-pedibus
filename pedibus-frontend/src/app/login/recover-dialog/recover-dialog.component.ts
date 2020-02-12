import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators} from '@angular/forms';
import { MatSnackBar } from '@angular/material';
import { AuthenticationService } from '../../authentication.service';
import { MatDialogRef } from '@angular/material/dialog';
import { handleError } from '../../utils';

@Component({
  selector: 'app-recover-dialog',
  templateUrl: './recover-dialog.component.html',
  styleUrls: ['./recover-dialog.component.css']
})
export class RecoverDialogComponent{
  form: FormGroup;

  constructor(public dialogRef: MatDialogRef<RecoverDialogComponent>,
              private fb: FormBuilder, 
              private authService: AuthenticationService,
              private _snackBar: MatSnackBar) {

      this.form = this.fb.group({
        email: ['', [Validators.required, Validators.email]]
      });
  }

  onRecoverClick(): void {
    const val = this.form.value;

    if (this.form.valid) {
      this.authService.recover(val.email)
          .subscribe(
              () => {
                this.dialogRef.close();
                this._snackBar.open("Recovery link sent to your email", "",
                    { panelClass: 'success-snackbar', duration: 5000 });
              },
              (error) => {
                handleError(error, this._snackBar);
              }
          );
    }
  }

}
