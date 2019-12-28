import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthenticationService } from './authentication.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

    constructor(private authenticationService: AuthenticationService) {}

    intercept(req: HttpRequest<any>,
              next: HttpHandler): Observable<HttpEvent<any>> {

        const idToken = localStorage.getItem("id_token");

        if (idToken) {
            console.log("Token found");
            const cloned = req.clone({
                headers: req.headers.set("Authorization",
                    "Bearer " + idToken)
            });

            return next.handle(cloned).pipe(
                catchError((err: HttpErrorResponse) => {
                    if (!(err.error instanceof ErrorEvent) && err.status == 401) {
                        // Client no longer authenticated (token invalid or expired), perform logout
                        this.authenticationService.logout();
                    }

                    throw err;
                })
            );
        }
        else {
            return next.handle(req);
        }
    }
}