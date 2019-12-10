import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule, EmailValidator, FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { HTTP_INTERCEPTORS } from '@angular/common/http';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatRadioModule } from '@angular/material/radio';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSelectModule } from '@angular/material/select';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AttendanceComponent } from './attendance/attendance.component';
import { StopDialogComponent } from './attendance/stop-dialog/stop-dialog.component';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { AttendanceService } from './attendance.service';
import { AuthenticationService } from './authentication.service';
import { AuthInterceptor } from './auth-interceptor';
import { UsersComponent, DialogNewUserComponent } from './users/users.component';
import { SimpleRideSelectorComponent } from './simple-ride-selector/simple-ride-selector.component';
import { RideSelectorComponent } from './ride-selector/ride-selector.component';
import { DialogUserLinesComponent } from './users/dialog-user-lines/dialog-user-lines.component';
import { AvailabilityComponent } from './availability/availability.component';
import { DeletionConfirmDialogComponent } from './availability/deletion-confirm-dialog/deletion-confirm-dialog.component';
import { PupilsComponent } from './pupils/pupils.component';
import { DialogPupilComponent } from './pupils/dialog-pupil/dialog-pupil.component';
// import { RidesComponent } from './rides/rides.component';


@NgModule({
  declarations: [
    AppComponent,
    AttendanceComponent,
    StopDialogComponent,
    LoginComponent,
    RegisterComponent,
    UsersComponent,
    SimpleRideSelectorComponent,
    RideSelectorComponent,
    DialogNewUserComponent,
    DialogUserLinesComponent,
    AvailabilityComponent,
    DeletionConfirmDialogComponent,
    PupilsComponent,
    DialogPupilComponent,
    // RidesComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MatCardModule,
    MatListModule,
    MatPaginatorModule,
    MatRadioModule,
    MatChipsModule,
    MatTabsModule,
    MatTooltipModule,
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
  entryComponents: [
    DialogNewUserComponent,
    DialogUserLinesComponent,
    StopDialogComponent,
    DeletionConfirmDialogComponent,
    DialogPupilComponent
  ]
})
export class AppModule { }
