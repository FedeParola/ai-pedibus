import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { AttendanceComponent } from './attendance/attendance.component';
import { AvailabilityComponent } from './availability/availability.component';
import { ReservationComponent } from './reservation/reservation.component';
import { LoginComponent } from './login/login.component';
import { UsersComponent } from './users/users.component';
import { PupilsComponent } from './pupils/pupils.component';
import { RidesComponent } from './rides/rides.component';
import { NotificationComponent } from './notification/notification.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'attendance', component: AttendanceComponent },
  { path: 'users', component: UsersComponent },
  { path: 'availability', component: AvailabilityComponent },
  { path: 'reservation', component: ReservationComponent },
  { path: 'pupils', component: PupilsComponent },
  { path: 'rides', component: RidesComponent },
  { path: 'notifications', component: NotificationComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
