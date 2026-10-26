import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin, of, switchMap } from 'rxjs';
import { EventCard } from '../../components/event-card/event-card';
import { AuthService } from '../../core/services/auth.service';
import { ApiEvent, EventService } from '../../core/services/event.service';
import { ALL_EVENTS_FILTER, EVENT_CATEGORIES } from '../../core/event-categories';

const REFRESH_MS = 60_000;

@Component({
  selector: 'app-events',
  imports: [FormsModule, EventCard, RouterLink],
  templateUrl: './events.html',
  styleUrl: './events.scss',
})
export class Events {
  private readonly eventService = inject(EventService);
  private readonly auth = inject(AuthService);
  private readonly routeData = inject(ActivatedRoute).snapshot.data;

  title: string = this.routeData['title'];
  heading: string = this.routeData['heading'];
  // My Events lists only the student's own registrations; other roles keep the full list.
  readonly registeredOnly: boolean = this.routeData['registeredOnly'] === true && this.auth.hasRole('USER');
  // Organizer's My Events: own events in every status, resolved by the backend from the JWT.
  readonly ownOnly: boolean = this.routeData['registeredOnly'] === true && this.auth.hasRole('ORGANIZER');
  searchTerm = '';
  selectedCategory: string = ALL_EVENTS_FILTER;
  categories: string[] = [ALL_EVENTS_FILTER, ...EVENT_CATEGORIES];
  // Signals are required because the app is zoneless: plain fields set in async callbacks don't trigger change detection.
  readonly events = signal<ApiEvent[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal('');

  constructor() {
    this.load();
    // Statuses are derived by the backend from the clock, so re-fetch periodically (without a loading flash).
    const timer = setInterval(() => this.load(true), REFRESH_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  load(silent = false): void {
    if (!silent) {
      this.loading.set(true);
      this.errorMessage.set('');
    }
    const source$ = this.registeredOnly
      // The discovery list hides finished events, so registered events are loaded by id.
      ? this.eventService.myRegistrations().pipe(
          switchMap((registrations) => registrations.length === 0
            ? of<ApiEvent[]>([])
            : forkJoin(registrations.map((registration) => this.eventService.get(registration.eventId)))))
      : this.ownOnly ? this.eventService.listMine() : this.eventService.list();
    source$.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (events) => {
        this.events.set(events);
        this.errorMessage.set('');
      },
      error: () => {
        if (silent) return;
        this.errorMessage.set(this.registeredOnly
          ? 'Could not load your events. Please try again.'
          : this.ownOnly
            ? 'Could not load your events. Please try again.'
            : 'Could not load events. Start the backend and refresh the page.');
      },
    });
  }

  get filteredEvents(): ApiEvent[] {
    return this.events().filter((event) => {
      const text = `${event.title} ${event.organizer} ${event.location} ${event.category}`.toLowerCase();
      return text.includes(this.searchTerm.toLowerCase())
        && (this.selectedCategory === ALL_EVENTS_FILTER || event.category === this.selectedCategory);
    });
  }
}