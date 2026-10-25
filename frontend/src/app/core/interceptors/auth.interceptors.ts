import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const API_URL = 'http://localhost:8081/api/';
const AUTH_URL = API_URL + 'auth/';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token;
  const isApi = req.url.startsWith(API_URL);

  const outgoing = token && isApi && !req.headers.has('Authorization')
    ? req.clone({ setHeaders: { Authorization: 'Bearer ' + token } })
    : req;

  return next(outgoing).pipe(
    catchError((error: unknown) => {
      // A 401 on a protected call means the token is no longer valid; wrong passwords come from /auth/ and are excluded.
      if (error instanceof HttpErrorResponse && error.status === 401 && isApi
          && !req.url.startsWith(AUTH_URL) && outgoing.headers.has('Authorization')) {
        auth.expireSession();
      }
      return throwError(() => error);
    }),
  );
};
