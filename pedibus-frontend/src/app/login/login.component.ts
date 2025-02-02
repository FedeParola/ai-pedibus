import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthenticationService } from '../authentication.service';
import { MatSnackBar } from '@angular/material';
import { HttpErrorResponse } from '@angular/common/http';
import { AppComponent } from '../app.component';
import { MatDialog, MatDialogRef, MatDialogConfig } from '@angular/material/dialog';
import { RecoverDialogComponent } from './recover-dialog/recover-dialog.component';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  form: FormGroup;
  loginButtonDisabled: boolean;

  constructor(private _snackBar: MatSnackBar,
              public dialog: MatDialog,
              private fb: FormBuilder, 
              private authService: AuthenticationService, 
              private router: Router,
              private appComponent: AppComponent) {

      this.form = this.fb.group({
          email: ['', Validators.required],
          password: ['', Validators.required]
      });
  }

  ngOnInit(): void {
    this.loginButtonDisabled = false;
    if(this.authService.isLoggedIn()){
      this.router.navigateByUrl('/reservation');
    }
  }

  login() {
      const val = this.form.value;

      if (val.email && val.password) {
        this.loginButtonDisabled = true;
        this.authService.login(val.email, val.password)
            .subscribe(
                () => {
                  this.router.navigateByUrl('/reservation');
                  this.appComponent.selectedView="Reservations";
                },
                (error) => {
                  this.loginButtonDisabled = false;
                  this.handleError(error);
                }
            );
      }
  }

  recoverPsw(){
    const dialogConfig = new MatDialogConfig();
    dialogConfig.width = '260px';

    this.dialog.open(RecoverDialogComponent, dialogConfig);
  }

  private handleError(error: HttpErrorResponse) {
    let errMsg: string;
    if (!(error.error instanceof ErrorEvent) && error.status == 401) {
      /* Invalid username or password */
      errMsg = "Invalid username or password!"
      
    } else {
      /* All other errors*/
      errMsg = "Error in the communication with the server!"
    }

    this._snackBar.open(errMsg, "", { panelClass: 'error-snackbar', duration: 5000 });
  };
}