import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { AttendanceComponent } from './attendance/attendance.component';
import { AvailabilityComponent } from './availability/availability.component';
import { ReservationComponent } from './reservation/reservation.component';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { UsersComponent } from './users/users.component';
// import { RidesComponent } from './rides/rides.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'attendance', component: AttendanceComponent },
  { path: 'users', component: UsersComponent },
  { path: 'availability', component: AvailabilityComponent },
  { path: 'reservation', component: ReservationComponent }
  // { path: 'rides', component: RidesComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
