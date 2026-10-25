import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin, map } from 'rxjs';
import { EventCard } from '../../components/event-card/event-card';
import { AuthService } from '../../core/services/auth.service';
import { ApiEvent, EventService } from '../../core/services/event.service';

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
  searchTerm = '';
  selectedCategory = 'All events';
  categories = ['All events', 'Technology', 'Social', 'Workshops', 'Volunteering'];
  // Signals are required because the app is zoneless: plain fields set in async callbacks don't trigger change detection.
  readonly events = signal<ApiEvent[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal('');

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    const source$ = this.registeredOnly
      ? forkJoin([this.eventService.list(), this.eventService.myRegistrations()]).pipe(
          map(([events, registrations]) => {
            const ids = new Set(registrations.map((registration) => registration.eventId));
            return events.filter((event) => ids.has(event.id));
          }))
      : this.eventService.list();
    source$.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (events) => this.events.set(events),
      error: () => this.errorMessage.set(this.registeredOnly
        ? 'Could not load your events. Please try again.'
        : 'Could not load events. Start the backend and refresh the page.'),
    });
  }

  get filteredEvents(): ApiEvent[] {
    return this.events().filter((event) => {
      const text = `${event.title} ${event.organizer} ${event.location} ${event.category}`.toLowerCase();
      return text.includes(this.searchTerm.toLowerCase())
        && (this.selectedCategory === 'All events' || event.category === this.selectedCategory);
    });
  }
}