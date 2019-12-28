import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material';

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