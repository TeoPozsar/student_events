import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiEvent, EventRequest, EventService } from '../../core/services/event.service';

const POSTER_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const POSTER_MAX_BYTES = 5 * 1024 * 1024;

@Component({
  selector: 'app-manage-events',
  imports: [FormsModule],
  templateUrl: './manage-events.html',
  styleUrl: './manage-events.scss',
})
export class ManageEvents {
  private readonly eventService = inject(EventService);

  // Signals: this app is zoneless, so async results must go through signals to re-render.
  readonly events = signal<ApiEvent[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly posterPreview = signal<string | null>(null);
  readonly posterError = signal('');
  readonly formError = signal('');
  readonly saving = signal(false);
  readonly deleting = signal(false);
  readonly toast = signal<{ type: 'success' | 'error'; text: string } | null>(null);
  readonly deleteTarget = signal<ApiEvent | null>(null);
  readonly failedPosters = signal<ReadonlySet<number>>(new Set());
  readonly total = computed(() => this.events().length);
  readonly upcoming = computed(() => this.events().filter((e) => new Date(e.startDateTime).getTime() >= Date.now()).length);
  readonly attendees = computed(() => this.events().reduce((sum, e) => sum + (e.attendeeCount ?? 0), 0));
  private readonly formDialog = viewChild<ElementRef<HTMLDialogElement>>('formDialog');
  private readonly deleteDialog = viewChild<ElementRef<HTMLDialogElement>>('deleteDialog');
  private toastTimer: ReturnType<typeof setTimeout> | undefined;
  eventForm: EventRequest = this.emptyEvent();
  editingId: number | null = null;
  private posterFile: File | null = null;
  private removeExistingPoster = false;
  private currentPosterSrc: string | null = null;

  constructor() {
    this.loadEvents();
  }

  loadEvents(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.eventService.list().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (events) => this.events.set(events),
      error: () => this.errorMessage.set('Could not load events. Start the backend and refresh the page.'),
    });
  }

  onPosterSelected(input: HTMLInputElement): void {
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;

    if (!POSTER_TYPES.includes(file.type)) {
      this.posterError.set('Poster must be a JPEG, PNG or WEBP image.');
      return;
    }
    if (file.size > POSTER_MAX_BYTES) {
      this.posterError.set('Poster must be at most 5 MB.');
      return;
    }

    this.posterError.set('');
    this.revokePreview();
    this.posterFile = file;
    this.posterPreview.set(URL.createObjectURL(file));
  }

  removePoster(): void {
    this.revokePreview();
    this.posterFile = null;
    this.posterError.set('');
    this.removeExistingPoster = this.currentPosterSrc !== null;
    this.posterPreview.set(null);
  }

  posterSrc(event: ApiEvent): string | null {
    return this.failedPosters().has(event.id) ? null : this.eventService.posterSrc(event);
  }

  onPosterError(event: ApiEvent): void {
    this.failedPosters.update((set) => new Set(set).add(event.id));
  }

  formatDate(value: string): string {
    return new Date(value).toLocaleString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });
  }

  openCreate(): void {
    this.cancelEdit();
    this.formDialog()?.nativeElement.showModal();
  }

  closeForm(): void {
    if (this.saving()) return;
    this.formDialog()?.nativeElement.close();
    this.cancelEdit();
  }

  saveEvent(): void {
    if (this.saving()) return;
    this.formError.set('');
    this.saving.set(true);
    const editing = this.editingId !== null;
    const poster = { file: this.posterFile, remove: this.removeExistingPoster };
    const request = this.editingId === null
      ? this.eventService.create(this.eventForm, poster)
      : this.eventService.update(this.editingId, this.eventForm, poster);
    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.formDialog()?.nativeElement.close();
        this.cancelEdit();
        this.failedPosters.set(new Set());
        this.showToast('success', editing ? 'Event updated.' : 'Event created.');
        this.loadEvents();
      },
      error: (error) => this.formError.set(this.message(error)),
    });
  }

  editEvent(event: ApiEvent): void {
    this.editingId = event.id;
    this.eventForm = {
      title: event.title, description: event.description, location: event.location,
      category: event.category, startDateTime: event.startDateTime.slice(0, 16), capacity: event.capacity,
    };
    this.resetPoster();
    this.currentPosterSrc = this.eventService.posterSrc(event);
    this.posterPreview.set(this.currentPosterSrc);
    this.formError.set('');
    this.formDialog()?.nativeElement.showModal();
  }

  askDelete(event: ApiEvent): void {
    this.deleteTarget.set(event);
    this.deleteDialog()?.nativeElement.showModal();
  }

  cancelDelete(): void {
    if (this.deleting()) return;
    this.deleteDialog()?.nativeElement.close();
    this.deleteTarget.set(null);
  }

  confirmDelete(): void {
    const target = this.deleteTarget();
    if (!target || this.deleting()) return;
    this.deleting.set(true);
    this.eventService.delete(target.id).pipe(finalize(() => this.deleting.set(false))).subscribe({
      next: () => {
        this.deleteDialog()?.nativeElement.close();
        this.deleteTarget.set(null);
        this.showToast('success', `"${target.title}" was deleted.`);
        this.loadEvents();
      },
      error: (error) => {
        this.deleteDialog()?.nativeElement.close();
        this.deleteTarget.set(null);
        this.showToast('error', this.message(error));
      },
    });
  }

  dismissToast(): void {
    this.toast.set(null);
  }

  private showToast(type: 'success' | 'error', text: string): void {
    clearTimeout(this.toastTimer);
    this.toast.set({ type, text });
    this.toastTimer = setTimeout(() => this.toast.set(null), 5000);
  }

  cancelEdit(): void {
    this.editingId = null;
    this.eventForm = this.emptyEvent();
    this.resetPoster();
  }

  private resetPoster(): void {
    this.revokePreview();
    this.posterFile = null;
    this.removeExistingPoster = false;
    this.currentPosterSrc = null;
    this.posterError.set('');
    this.posterPreview.set(null);
  }

  private revokePreview(): void {
    const preview = this.posterPreview();
    if (preview?.startsWith('blob:')) {
      URL.revokeObjectURL(preview);
    }
  }

  private emptyEvent(): EventRequest {
    return { title: '', description: '', location: '', category: 'Technology', startDateTime: '', capacity: 20 };
  }

  private message(error: { status?: number; error?: { error?: string; message?: string } }): string {
    if (error.status === 413) return 'Poster is too large (max 5 MB).';
    if (error.status === 415) return 'Poster must be a JPEG, PNG or WEBP image.';
    return error.error?.message ?? error.error?.error ?? 'Something went wrong. Please try again.';
  }
}
