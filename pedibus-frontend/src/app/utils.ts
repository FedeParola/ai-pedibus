import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material';
import * as moment from 'moment';

export function handleError(error: HttpErrorResponse, snackBar: MatSnackBar) {
  let errMsg;

  if (error.error instanceof ErrorEvent) {
    errMsg = "Error in the communication with the server";

  } else {
    if (error.status == 401) {
      // auth-interceptor takes care of logging out
      errMsg = "Authentication time expired";
    
    } else {
      errMsg = error.error.message;
    }
  }

  snackBar.open(errMsg, "", { panelClass: 'error-snackbar', duration: 5000 });
}

/**
 * Returns the index of a id-based element into a list, -1 if not present
 * @param element
 * @param list
 */
export function findElement(element, list): number {
  for (let [i, e] of list.entries()) {
    if (element.id == e.id) {
      return i;
    }
  }

  return -1;
}

export function findClosestRide(rides): number {
  let curDate = moment().format('YYYY-MM-DD');
  let closestRide = rides.length-1;

  while (closestRide > 0 && rides[closestRide].date >= curDate) {
    closestRide--;
  }

  if (rides[closestRide].date < curDate && !(closestRide === rides.length-1)) {
    closestRide++;
  }
  
  return closestRide;
}

export function findNextClosestRide(rides): number {
  let curDate = moment().format('YYYY-MM-DD');
  let closestRide = rides.length-1;

  while (closestRide > 0 && rides[closestRide].date > curDate) {
    closestRide--;
  }

  if (rides[closestRide].date <= curDate && !(closestRide === rides.length-1)) {
    closestRide++;
  }
  
  return closestRide;
}