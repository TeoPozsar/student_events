import { computed, effect, inject, Injectable, signal, untracked } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { concat, Observable, tap } from 'rxjs';
import { AuthService } from './auth.service';

export interface Profile {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  university: string | null;
  location: string | null;
  hasProfilePicture: boolean;
}

export interface ProfileChanges {
  firstName: string;
  lastName: string;
  university: string;
  location: string;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly baseUrl = 'http://localhost:8081/api/users/me';

  readonly profile = signal<Profile | null>(null);
  // Object URL of the saved picture; fetched with the JWT because the endpoint is authenticated.
  readonly pictureUrl = signal<string | null>(null);
  readonly initial = computed(() => (this.auth.user()?.firstName ?? '?').charAt(0).toUpperCase());

  private readonly userId = computed(() => this.auth.user()?.id ?? null);

  constructor() {
    // Follows the signed-in user (by id, so profile edits do not retrigger it): load on login/restore, wipe on logout.
    effect(() => {
      const id = this.userId();
      untracked(() => id === null ? this.reset() : this.load());
    });
  }

  readonly loadFailed = signal(false);

  load(): void {
    this.loadFailed.set(false);
    this.http.get<Profile>(this.baseUrl).subscribe({
      next: (profile) => this.apply(profile),
      error: () => this.loadFailed.set(true),
    });
  }

  save(changes: ProfileChanges, picture: File | null): Observable<Profile> {
    const steps: Observable<Profile>[] = [this.http.put<Profile>(this.baseUrl, changes)];
    if (picture) {
      const data = new FormData();
      data.append('picture', picture);
      steps.push(this.http.post<Profile>(`${this.baseUrl}/picture`, data));
    }
    return concat(...steps).pipe(tap((profile) => this.apply(profile)));
  }

  removePicture(): Observable<Profile> {
    return this.http.delete<Profile>(`${this.baseUrl}/picture`).pipe(tap((profile) => this.apply(profile)));
  }

  private apply(profile: Profile): void {
    this.profile.set(profile);
    this.auth.updateUser({ firstName: profile.firstName, lastName: profile.lastName });

    if (!profile.hasProfilePicture) {
      this.setPictureUrl(null);
      return;
    }
    this.http.get(`${this.baseUrl}/picture`, { responseType: 'blob' }).subscribe({
      // Ignore a late response if the user logged out meanwhile.
      next: (blob) => this.setPictureUrl(this.auth.user() ? URL.createObjectURL(blob) : null),
      error: () => this.setPictureUrl(null),
    });
  }

  private reset(): void {
    this.profile.set(null);
    this.setPictureUrl(null);
  }

  private setPictureUrl(url: string | null): void {
    const old = this.pictureUrl();
    if (old) URL.revokeObjectURL(old);
    this.pictureUrl.set(url);
  }
}
