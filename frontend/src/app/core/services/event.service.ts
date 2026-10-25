import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApiEvent {
  id: number;
  title: string;
  description: string;
  location: string;
  category: string;
  startDateTime: string;
  capacity: number;
  attendeeCount: number;
  organizer: string;
  posterUrl?: string | null;
}

export interface Registration {
  id: number;
  eventId: number;
  status: 'REGISTERED' | 'CANCELLED' | 'CHECKED_IN';
  registeredAt: string;
}

export interface EventRequest {
  title: string;
  description: string;
  location: string;
  category: string;
  startDateTime: string;
  capacity: number;
}

export interface PosterChange {
  file: File | null;
  remove: boolean;
}

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly apiHost = 'http://localhost:8081';
  private readonly baseUrl = `${this.apiHost}/api/events`;

  posterSrc(event: ApiEvent): string | null {
    if (!event.posterUrl) return null;
    return /^https?:\/\//.test(event.posterUrl) ? event.posterUrl : this.apiHost + event.posterUrl;
  }

  list(): Observable<ApiEvent[]> {
    return this.http.get<ApiEvent[]>(this.baseUrl);
  }

  get(id: number): Observable<ApiEvent> {
    return this.http.get<ApiEvent>(`${this.baseUrl}/${id}`);
  }

  myRegistrations(): Observable<Registration[]> {
    return this.http.get<Registration[]>(`${this.apiHost}/api/registrations/me`);
  }

  register(eventId: number): Observable<Registration> {
    return this.http.post<Registration>(`${this.baseUrl}/${eventId}/register`, {});
  }

  cancelRegistration(eventId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${eventId}/register`);
  }

  create(event: EventRequest, poster?: PosterChange): Observable<ApiEvent> {
    return this.http.post<ApiEvent>(this.baseUrl, this.formData(event, poster), { withCredentials: true });
  }

  update(id: number, event: EventRequest, poster?: PosterChange): Observable<ApiEvent> {
    const params = poster?.remove && !poster.file ? '?removePoster=true' : '';
    return this.http.put<ApiEvent>(`${this.baseUrl}/${id}${params}`, this.formData(event, poster), { withCredentials: true });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`, { withCredentials: true });
  }

  // The browser sets the multipart Content-Type and boundary itself; do not set it manually.
  private formData(event: EventRequest, poster?: PosterChange): FormData {
    const data = new FormData();
    data.append('event', new Blob([JSON.stringify(event)], { type: 'application/json' }));
    if (poster?.file) {
      data.append('poster', poster.file);
    }
    return data;
  }
}
