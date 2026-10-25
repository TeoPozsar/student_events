import { inject } from '@angular/core';
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { NoticeService } from '../services/notice.service';

// All guards wait for the saved session to be checked first, so a page refresh
// on a protected URL neither bounces the user to /login nor loops.

export const authGuard: CanActivateChildFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.restoreSession().pipe(
    map(() => auth.user()
      ? true
      : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } })),
  );
};

// Login/register are public, but an already signed-in user is sent on to the app.
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.restoreSession().pipe(
    map(() => auth.user() ? router.createUrlTree(['/events']) : true),
  );
};

// Usage: canActivate: [roleGuard], data: { roles: ['ORGANIZER', 'ADMIN'] }
export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const notice = inject(NoticeService);
  const roles = (route.data['roles'] as string[] | undefined) ?? [];

  return auth.restoreSession().pipe(
    map(() => {
      if (!auth.user()) return router.createUrlTree(['/login']);
      if (roles.length === 0 || auth.hasRole(...roles)) return true;
      notice.show('You do not have permission to open that page.');
      return router.createUrlTree(['/events']);
    }),
  );
};
