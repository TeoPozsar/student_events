import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, map, Observable, of, shareReplay, tap } from 'rxjs';
import { NoticeService } from './notice.service';

export type Role = 'USER' | 'ORGANIZER' | 'ADMIN';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
}

interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
}

interface MeResponse extends User {}

export const MANAGER_ROLES: Role[] = ['ORGANIZER', 'ADMIN'];

const TOKEN_KEY = 'auth_token';
const USER_KEY = 'auth_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly notice = inject(NoticeService);
  private readonly apiUrl = 'http://localhost:8081/api';
  private restore$?: Observable<User | null>;

  readonly user = signal<User | null>(null);
  token: string | null = null;
  // False until the saved session has been checked, so the UI does not flash the login page.
  readonly initialized = signal(false);

  readonly isManager = computed(() => this.hasRole(...MANAGER_ROLES));
  readonly workspaceLabel = computed(() => {
    switch (this.user()?.role) {
      case 'ORGANIZER': return 'Organizer Workspace';
      case 'ADMIN': return 'Admin Workspace';
      default: return 'Student Space';
    }
  });

  hasRole(...roles: string[]): boolean {
    const role = this.user()?.role;
    return !!role && roles.includes(role);
  }

  register(data: { firstName: string; lastName: string; email: string; password: string }): Observable<User> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/register`, data).pipe(
      map((response) => this.startSession(response)),
    );
  }

  login(email: string, password: string): Observable<User> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, { email, password }).pipe(
      map((response) => this.startSession(response)),
    );
  }

  // Resolves once the saved session has been checked; safe to call many times (guards, app startup).
  restoreSession(): Observable<User | null> {
    this.restore$ ??= this.checkSavedSession().pipe(shareReplay(1));
    return this.restore$;
  }

  // Stateless JWT: the backend has no logout endpoint, so logging out is client-side only.
  logout(): void {
    this.clearSession();
    this.router.navigate(['/login'], { replaceUrl: true }).catch(() => undefined);
  }

  // Called when the backend rejects the token (expired or invalid) during normal use.
  expireSession(): void {
    if (!this.token && !this.user()) return;
    this.clearSession();
    this.notice.show('Your session has expired. Please log in again.');
    this.router.navigate(['/login'], { replaceUrl: true }).catch(() => undefined);
  }

  updateUser(changes: Partial<User>): void {
    const current = this.user();
    if (!current) return;
    const updated = { ...current, ...changes };
    this.user.set(updated);
    this.writeStorage(USER_KEY, JSON.stringify(updated));
  }

  // The saved token is verified with GET /api/users/me, which also refreshes the user's details and role.
  private checkSavedSession(): Observable<User | null> {
    const token = this.readStorage(TOKEN_KEY);
    const saved = this.readSavedUser();
    if (!token || !saved) {
      this.clearSession();
      this.initialized.set(true);
      return of(null);
    }

    this.token = token;
    return this.http.get<MeResponse>(`${this.apiUrl}/users/me`).pipe(
      map((me): User | null => ({ id: me.id, firstName: me.firstName, lastName: me.lastName, email: me.email, role: me.role })),
      catchError((error: unknown) => {
        // Only a rejected token ends the session; a temporarily unreachable backend must not log users out.
        const rejected = error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403);
        return of(rejected ? null : saved);
      }),
      tap((restored) => {
        if (restored) {
          this.user.set(restored);
          this.writeStorage(USER_KEY, JSON.stringify(restored));
        } else {
          this.clearSession();
        }
        this.initialized.set(true);
      }),
    );
  }

  private startSession(response: AuthResponse): User {
    const user: User = {
      id: response.userId,
      firstName: response.firstName,
      lastName: response.lastName,
      email: response.email,
      role: response.role,
    };
    this.token = response.token;
    this.user.set(user);
    this.initialized.set(true);
    this.writeStorage(TOKEN_KEY, response.token);
    this.writeStorage(USER_KEY, JSON.stringify(user));
    return user;
  }

  // In-memory state is cleared first so a storage failure can never leave the user logged in.
  private clearSession(): void {
    this.token = null;
    this.user.set(null);
    try {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
    } catch {
      // Storage unavailable: nothing to clean up.
    }
  }

  private readSavedUser(): User | null {
    try {
      return JSON.parse(this.readStorage(USER_KEY) ?? 'null');
    } catch {
      return null;
    }
  }

  private readStorage(key: string): string | null {
    try {
      return sessionStorage.getItem(key);
    } catch {
      return null;
    }
  }

  private writeStorage(key: string, value: string): void {
    try {
      sessionStorage.setItem(key, value);
    } catch {
      // Session still works in memory for this page load.
    }
  }
}
