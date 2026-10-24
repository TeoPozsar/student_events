import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService } from './auth.service';
import { ApiEvent, EventRequest, EventService } from './event.service';

interface EventItem extends ApiEvent {
  date: string;
  time: string;
}

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly eventService = inject(EventService);
  readonly authService = inject(AuthService);
  activeView: 'discover' | 'my-events' | 'organizer' = 'discover';
  searchTerm = '';
  selectedCategory = 'All events';
  events: EventItem[] = [];
  categories = ['All events', 'Technology', 'Social', 'Workshops', 'Volunteering'];
  loading = true;
  errorMessage = '';
  authMode: 'login' | 'register' = 'login';
  authError = '';
  authForm = { firstName: '', lastName: '', email: '', password: '' };
  eventForm: EventRequest = this.emptyEvent();
  editingId: number | null = null;
  languageDropdownOpen = false;
  settingsDropdownOpen = false;

  constructor() {
    this.loadEvents();
    this.authService.restoreSession().subscribe({
      error: () => { /* A visitor may browse without an account. */ },
    });
  }

  get filteredEvents(): EventItem[] {
    return this.events.filter((event) => {
      const text = `${event.title} ${event.organizer} ${event.location} ${event.category}`.toLowerCase();
      return text.includes(this.searchTerm.toLowerCase())
        && (this.selectedCategory === 'All events' || event.category === this.selectedCategory);
    });
  }

  setView(view: 'discover' | 'my-events' | 'organizer'): void {
    this.activeView = view;
  }

  loadEvents(): void {
    this.loading = true;
    this.eventService.list().pipe(finalize(() => this.loading = false)).subscribe({
      next: (events) => this.events = events.map((event) => this.displayEvent(event)),
      error: () => this.errorMessage = 'Could not load events. Start the backend and refresh the page.',
    });
  }

  submitAuth(): void {
    this.authError = '';
    const request = this.authMode === 'login'
      ? this.authService.login(this.authForm.email, this.authForm.password)
      : this.authService.register(this.authForm);
    request.subscribe({
      next: () => {
        if (this.authMode === 'register') {
          this.authService.login(this.authForm.email, this.authForm.password).subscribe({
            error: (error) => this.authError = this.message(error),
          });
        }
        this.authForm.password = '';
      },
      error: (error) => this.authError = this.message(error),
    });
  }

  logout(): void {
    this.authService.logout().subscribe({ error: () => this.authService.user = null });
  }

  saveEvent(): void {
    this.errorMessage = '';
    const request = this.editingId === null
      ? this.eventService.create(this.eventForm)
      : this.eventService.update(this.editingId, this.eventForm);
    request.subscribe({
      next: () => {
        this.cancelEdit();
        this.loadEvents();
      },
      error: (error) => this.errorMessage = this.message(error),
    });
  }

  editEvent(event: EventItem): void {
    this.editingId = event.id;
    this.eventForm = {
      title: event.title, description: event.description, location: event.location,
      category: event.category, startDateTime: event.startDateTime.slice(0, 16), capacity: event.capacity,
    };
    this.activeView = 'organizer';
  }

  deleteEvent(id: number): void {
    if (!confirm('Delete this event?')) return;
    this.eventService.delete(id).subscribe({
      next: () => this.loadEvents(),
      error: (error) => this.errorMessage = this.message(error),
    });
  }

  cancelEdit(): void {
    this.editingId = null;
    this.eventForm = this.emptyEvent();
  }

  private displayEvent(event: ApiEvent): EventItem {
    const date = new Date(event.startDateTime);
    return { ...event, date: date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }).toUpperCase(),
      time: date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) };
  }

  private emptyEvent(): EventRequest {
    return { title: '', description: '', location: '', category: 'Technology', startDateTime: '', capacity: 20 };
  }

  private message(error: { error?: { error?: string } }): string {
    return error.error?.error ?? 'Something went wrong. Please try again.';
  }
}
