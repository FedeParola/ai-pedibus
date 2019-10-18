import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule, EmailValidator } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { HTTP_INTERCEPTORS } from '@angular/common/http';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatDialogModule} from '@angular/material/dialog';
import {MatSelectModule} from '@angular/material/select';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AttendanceComponent } from './attendance/attendance.component';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { AttendanceService } from './attendance.service';
import { AuthenticationService } from './authentication.service';
import { AuthInterceptor } from './auth-interceptor';
import { UsersComponent, DialogNewUserComponent } from './users/users.component';
import { SimpleRideSelectorComponent } from './simple-ride-selector/simple-ride-selector.component';
import { RideSelectorComponent } from './ride-selector/ride-selector.component';
import { DialogUserLinesComponent } from './users/dialog-user-lines/dialog-user-lines.component';

@NgModule({
  declarations: [
    AppComponent,
    AttendanceComponent,
    LoginComponent,
    RegisterComponent,
    UsersComponent,
    SimpleRideSelectorComponent,
    RideSelectorComponent,
    DialogNewUserComponent,
    DialogUserLinesComponent
  ],
  imports: [
    BrowserModule,
    ReactiveFormsModule,
    HttpClientModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MatCardModule,
    MatListModule,
    MatPaginatorModule,
    MatChipsModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
    MatIconModule,
    MatMenuModule,
    MatToolbarModule,
    MatDialogModule,
    MatSelectModule
  ],
  providers: [
    AttendanceService,
    AuthenticationService,
    { provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true }
  ],
  bootstrap: [AppComponent],
  entryComponents: [DialogNewUserComponent, DialogUserLinesComponent]
})
export class AppModule { }
