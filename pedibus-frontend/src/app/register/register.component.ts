import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators, EmailValidator, AsyncValidatorFn } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthenticationService } from '../authentication.service';
import { MatSnackBar } from '@angular/material';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  form: FormGroup;
  registerButtonDisabled: boolean;

  constructor(private _snackBar: MatSnackBar,
    private fb: FormBuilder, 
    private authService: AuthenticationService,
    private router: Router) {
      this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.pattern('^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).{6,32}$')]],
      confPassword: ['', Validators.required]
    },{
      validator: this.matchingConfirmPasswords
    });
  }

  ngOnInit() {
    this.registerButtonDisabled = false;
  }

  register() {
    const val = this.form.value;

    if (this.form.valid) {
      this.registerButtonDisabled = true;
      this.authService.register(val.email, val.password, val.confPassword)
          .subscribe(
              () => {
                console.log("User is registered");
                this.router.navigateByUrl('/login');
                this._snackBar.open("Account created, please confirm your e-mail address", "",
                    { panelClass: 'success-snackbar', duration: 5000 });
              },
              () => {
                this.registerButtonDisabled = false;
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

  matchingConfirmPasswords(form: FormGroup) { 
    const val = form.value;
    if (val.password === val.confPassword) { 
      return null; 
    } 
    else { 
        return form.controls['confPassword'].setErrors({ passwordNotEquivalent: true }); 
    } 
  }
}

