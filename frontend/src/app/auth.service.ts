import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8081/api/auth';
  user: User | null = null;

  register(data: { firstName: string; lastName: string; email: string; password: string }): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/register`, data);
  }

  login(email: string, password: string): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/login`, { email, password }, { withCredentials: true })
      .pipe(tap((user) => this.user = user));
  }

  restoreSession(): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/me`, { withCredentials: true })
      .pipe(tap((user) => this.user = user));
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, {}, { withCredentials: true })
      .pipe(tap(() => this.user = null));
  }
}
