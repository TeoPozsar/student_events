import { Component, computed, inject, input, output, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ApiEvent, EventService } from '../../core/services/event.service';

@Component({
  selector: 'app-event-card',
  templateUrl: './event-card.html',
  styleUrl: './event-card.scss',
})
export class EventCard {
  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);
  private readonly failedSrc = signal<string | null>(null);

  event = input.required<ApiEvent>();
  showActions = input(false);
  edit = output<ApiEvent>();
  remove = output<number>();

  posterSrc = computed(() => {
    const src = this.eventService.posterSrc(this.event());
    return src && src !== this.failedSrc() ? src : null;
  });

  open(domEvent: Event): void {
    // Ignore clicks on inner controls such as Edit/Delete.
    if ((domEvent.target as HTMLElement).closest('button, a, input, select, textarea')) return;
    domEvent.preventDefault();
    void this.router.navigate(['/events', this.event().id]);
  }

  onPosterError(): void {
    this.failedSrc.set(this.eventService.posterSrc(this.event()));
  }

  date = computed(() =>
    new Date(this.event().startDateTime).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }).toUpperCase());
  time = computed(() =>
    new Date(this.event().startDateTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
}
