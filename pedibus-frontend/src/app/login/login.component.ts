import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthenticationService } from '../authentication.service';
import { MatSnackBar } from '@angular/material';
import { HttpErrorResponse } from '@angular/common/http';
import { AppComponent } from '../app.component';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  form: FormGroup;
  loginButtonDisabled: boolean;

  constructor(private _snackBar: MatSnackBar,
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
  }

  login() {
      const val = this.form.value;

      if (val.email && val.password) {
        this.loginButtonDisabled = true;
        this.authService.login(val.email, val.password)
            .subscribe(
                () => {
                  console.log("User is logged in");
                  this.router.navigateByUrl('/attendance');
                  this.appComponent.logoutDisabled=false;
                },
                (error) => {
                  this.loginButtonDisabled = false;
                  this.handleError(error);
                }
            );
      }
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