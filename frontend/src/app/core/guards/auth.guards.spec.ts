import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { firstValueFrom, isObservable, Observable } from 'rxjs';
import { authGuard, guestGuard, roleGuard } from './auth.guards';
import { AuthService } from '../services/auth.service';

const ME = (role: string) => ({ id: 1, firstName: 'A', lastName: 'B', email: 'a@b.c', role });

describe('route guards', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  function run(guard: typeof authGuard | typeof guestGuard | typeof roleGuard, data: Record<string, unknown> = {}) {
    return TestBed.runInInjectionContext(() =>
      (guard as any)({ data } as any, { url: '/manage' } as any) as Observable<boolean | UrlTree>);
  }

  function seedSession(role: string) {
    sessionStorage.setItem('auth_token', 'token');
    sessionStorage.setItem('auth_user', JSON.stringify(ME(role)));
  }

  it('redirects anonymous users to /login with a return URL', async () => {
    const result = run(authGuard);
    expect(isObservable(result)).toBe(true);
    const tree = (await firstValueFrom(result)) as UrlTree;
    expect(router.serializeUrl(tree)).toBe('/login?returnUrl=%2Fmanage');
  });

  it('lets a restored session through after refresh', async () => {
    seedSession('USER');
    const pending = firstValueFrom(run(authGuard));
    http.expectOne('http://localhost:8081/api/users/me').flush(ME('USER'));
    expect(await pending).toBe(true);
    expect(TestBed.inject(AuthService).user()?.email).toBe('a@b.c');
  });

  it('drops a session the backend rejects', async () => {
    seedSession('USER');
    const pending = firstValueFrom(run(authGuard));
    http.expectOne('http://localhost:8081/api/users/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(await pending).toBeInstanceOf(UrlTree);
    expect(sessionStorage.getItem('auth_token')).toBeNull();
  });

  it('blocks USER from organizer routes but allows ORGANIZER', async () => {
    seedSession('USER');
    const denied = firstValueFrom(run(roleGuard, { roles: ['ORGANIZER', 'ADMIN'] }));
    http.expectOne('http://localhost:8081/api/users/me').flush(ME('USER'));
    expect(router.serializeUrl((await denied) as UrlTree)).toBe('/events');
  });

  it('allows ORGANIZER on organizer routes', async () => {
    seedSession('ORGANIZER');
    const allowed = firstValueFrom(run(roleGuard, { roles: ['ORGANIZER', 'ADMIN'] }));
    http.expectOne('http://localhost:8081/api/users/me').flush(ME('ORGANIZER'));
    expect(await allowed).toBe(true);
  });

  it('keeps login public for anonymous users and redirects signed-in ones', async () => {
    expect(await firstValueFrom(run(guestGuard))).toBe(true);
  });

  it('logout clears token, user and storage', () => {
    seedSession('USER');
    const auth = TestBed.inject(AuthService);
    auth.restoreSession().subscribe();
    http.expectOne('http://localhost:8081/api/users/me').flush(ME('USER'));
    expect(auth.user()).not.toBeNull();
    auth.logout();
    expect(auth.user()).toBeNull();
    expect(auth.token).toBeNull();
    expect(sessionStorage.getItem('auth_token')).toBeNull();
    expect(sessionStorage.getItem('auth_user')).toBeNull();
  });
});
