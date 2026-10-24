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
}

export interface EventRequest {
  title: string;
  description: string;
  location: string;
  category: string;
  startDateTime: string;
  capacity: number;
}

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8081/api/events';

  list(): Observable<ApiEvent[]> {
    return this.http.get<ApiEvent[]>(this.baseUrl);
  }

  create(event: EventRequest): Observable<ApiEvent> {
    return this.http.post<ApiEvent>(this.baseUrl, event, { withCredentials: true });
  }

  update(id: number, event: EventRequest): Observable<ApiEvent> {
    return this.http.put<ApiEvent>(`${this.baseUrl}/${id}`, event, { withCredentials: true });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`, { withCredentials: true });
  }
}
