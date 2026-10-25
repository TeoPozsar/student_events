import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, Location } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { ApiEvent, EventService } from '../../core/services/event.service';
import { toFriendlyError } from '../../core/utils/http-error';

type LoadState = 'loading' | 'ready' | 'not-found' | 'error';

@Component({
  selector: 'app-event-details',
  imports: [DatePipe, RouterLink],
  templateUrl: './event-details.html',
  styleUrl: './event-details.scss',
})
export class EventDetails {
  private readonly eventService = inject(EventService);
  private readonly auth = inject(AuthService);
  private readonly location = inject(Location);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly idParam = toSignal(this.route.paramMap.pipe(map((params) => params.get('id'))), { initialValue: null });

  readonly event = signal<ApiEvent | null>(null);
  readonly state = signal<LoadState>('loading');
  readonly registered = signal(false);
  readonly submitting = signal(false);
  readonly feedback = signal<{ type: 'success' | 'error'; text: string } | null>(null);
  readonly posterFailed = signal(false);

  readonly isStudent = computed(() => this.auth.hasRole('USER'));
  readonly isManager = this.auth.isManager;
  readonly posterSrc = computed(() => {
    const event = this.event();
    return event && !this.posterFailed() ? this.eventService.posterSrc(event) : null;
  });
  readonly isFull = computed(() => {
    const event = this.event();
    return !!event && event.attendeeCount >= event.capacity;
  });

  private loadedId: number | null = null;

  constructor() {
    this.load();
  }

  load(): void {
    const raw = this.idParam();
    const id = raw !== null && /^\d+$/.test(raw) ? Number(raw) : NaN;
    this.feedback.set(null);
    this.posterFailed.set(false);
    if (!Number.isSafeInteger(id) || id <= 0) {
      this.state.set('not-found');
      return;
    }
    this.loadedId = id;
    this.state.set('loading');
    this.eventService.get(id).subscribe({
      next: (event) => {
        this.event.set(event);
        if (!this.isStudent()) {
          this.state.set('ready');
          return;
        }
        this.eventService.myRegistrations().subscribe({
          next: (registrations) => {
            this.registered.set(registrations.some((r) => r.eventId === id));
            this.state.set('ready');
          },
          error: () => this.state.set('error'),
        });
      },
      error: (error: unknown) => this.state.set(error instanceof HttpErrorResponse && error.status === 404 ? 'not-found' : 'error'),
    });
  }

  register(): void {
    const event = this.event();
    if (!event || this.submitting() || this.registered()) return;
    this.submitting.set(true);
    this.feedback.set(null);
    this.eventService.register(event.id).subscribe({
      next: () => {
        this.registered.set(true);
        this.submitting.set(false);
        this.event.set({ ...event, attendeeCount: event.attendeeCount + 1 });
        this.feedback.set({ type: 'success', text: 'You are registered for this event.' });
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        const message = error instanceof HttpErrorResponse && error.status === 409
          ? (error.error?.message ?? 'Registration is not possible for this event.')
          : toFriendlyError(error, 'Could not register. Please try again.').message;
        this.feedback.set({ type: 'error', text: message });
        // A conflict may mean the event filled up or we were already registered; resync.
        if (error instanceof HttpErrorResponse && error.status === 409 && this.loadedId !== null) this.load();
      },
    });
  }

  cancel(): void {
    const event = this.event();
    if (!event || this.submitting() || !this.registered()) return;
    this.submitting.set(true);
    this.feedback.set(null);
    this.eventService.cancelRegistration(event.id).subscribe({
      next: () => {
        this.registered.set(false);
        this.submitting.set(false);
        this.event.set({ ...event, attendeeCount: Math.max(0, event.attendeeCount - 1) });
        this.feedback.set({ type: 'success', text: 'Your registration was cancelled.' });
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.feedback.set({ type: 'error', text: toFriendlyError(error, 'Could not cancel. Please try again.').message });
      },
    });
  }

  back(): void {
    // history.length > 1 means there is an earlier page inside this tab.
    if (window.history.length > 1 && window.history.state?.navigationId > 1) {
      this.location.back();
    } else {
      void this.router.navigate(['/events']);
    }
  }
}
