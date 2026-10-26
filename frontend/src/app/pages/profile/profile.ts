import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { finalize } from 'rxjs';
import { ProfileService } from '../../core/services/profile.service';
import { toFriendlyError } from '../../core/utils/http-error';
import { ToastService } from '../../core/services/toast.service';

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_PICTURE_BYTES = 5 * 1024 * 1024;

@Component({
  selector: 'app-profile',
  imports: [FormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile {
  readonly profileService = inject(ProfileService);
  private readonly toasts = inject(ToastService);

  form = { firstName: '', lastName: '', university: '', location: '' };
  readonly saving = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');
  readonly pictureError = signal('');
  readonly serverErrors = signal<Record<string, string>>({});
  readonly pendingFile = signal<File | null>(null);
  readonly previewUrl = signal<string | null>(null);
  private edited = false;

  readonly shownPicture = computed(() => this.previewUrl() ?? this.profileService.pictureUrl());
  readonly roleLabel = computed(() => {
    const role = this.profileService.profile()?.role ?? '';
    return role.charAt(0) + role.slice(1).toLowerCase();
  });

  constructor() {
    // Fill the form when the saved profile arrives (and again after a save), unless the user is mid-edit.
    effect(() => {
      const profile = this.profileService.profile();
      if (profile && !this.edited) {
        this.form = {
          firstName: profile.firstName,
          lastName: profile.lastName,
          university: profile.university ?? '',
          location: profile.location ?? '',
        };
      }
    });
    inject(DestroyRef).onDestroy(() => this.clearPreview());
  }

  markEdited(): void {
    this.edited = true;
    this.successMessage.set('');
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    this.pictureError.set('');
    this.successMessage.set('');
    if (!file) return;

    if (!ALLOWED_TYPES.includes(file.type)) {
      this.pictureError.set('Please choose a JPEG, PNG or WEBP image.');
      return;
    }
    if (file.size > MAX_PICTURE_BYTES) {
      this.pictureError.set('The picture must be at most 5 MB.');
      return;
    }

    this.clearPreview();
    this.pendingFile.set(file);
    this.previewUrl.set(URL.createObjectURL(file));
  }

  discardPicture(): void {
    this.clearPreview();
    this.pictureError.set('');
  }

  removeSavedPicture(): void {
    if (this.saving()) return;
    this.saving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.profileService.removePicture().pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => this.toasts.success('Profile picture removed successfully!'),
      error: (error: unknown) => this.errorMessage.set(toFriendlyError(error, 'Could not remove the picture.').message),
    });
  }

  save(ngForm: NgForm): void {
    if (this.saving()) return;
    this.successMessage.set('');
    this.errorMessage.set('');
    this.serverErrors.set({});

    if (ngForm.invalid) {
      ngForm.control.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const hadPicture = this.pendingFile() !== null;
    this.profileService.save(this.form, this.pendingFile()).pipe(
      finalize(() => this.saving.set(false)),
    ).subscribe({
      next: () => {
        this.edited = false;
        this.clearPreview();
      },
      // save() emits once per request (details, then picture); only completion means everything succeeded.
      complete: () => {
        this.toasts.success('Profile updated successfully!');
        if (hadPicture) this.toasts.success('Profile picture updated successfully!');
      },
      error: (error: unknown) => {
        const friendly = toFriendlyError(error, 'Could not save your profile. Please try again.');
        this.errorMessage.set(friendly.message);
        this.serverErrors.set(friendly.fieldErrors);
      },
    });
  }

  private clearPreview(): void {
    const url = this.previewUrl();
    if (url) URL.revokeObjectURL(url);
    this.previewUrl.set(null);
    this.pendingFile.set(null);
  }
}
