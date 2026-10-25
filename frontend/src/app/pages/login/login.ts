import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { NoticeService } from '../../core/services/notice.service';
import { toFriendlyError } from '../../core/utils/http-error';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly notice = inject(NoticeService);

  readonly authMode: 'login' | 'register' = this.route.snapshot.data['mode'] === 'register' ? 'register' : 'login';
  readonly loading = signal(false);
  readonly authError = signal('');
  readonly serverErrors = signal<Record<string, string>>({});
  authForm = { firstName: '', lastName: '', email: '', password: '' };

  constructor() {
    inject(DestroyRef).onDestroy(() => this.notice.clear());
  }

  submitAuth(form: NgForm): void {
    if (this.loading()) return;
    this.authError.set('');
    this.serverErrors.set({});
    this.notice.clear();

    if (form.invalid) {
      form.control.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    const email = this.authForm.email.trim();
    const request = this.authMode === 'login'
      ? this.authService.login(email, this.authForm.password)
      : this.authService.register({ ...this.authForm, email });

    request.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => this.router.navigateByUrl(this.returnUrl()),
      error: (error: unknown) => {
        const friendly = toFriendlyError(error);
        this.serverErrors.set(friendly.fieldErrors);
        this.authError.set(this.authMode === 'login' && friendly.message === 'Please correct the highlighted fields.'
          ? 'Enter a valid email and password.'
          : friendly.message);
      },
    });
  }

  switchMode(): void {
    this.router.navigate([this.authMode === 'login' ? '/register' : '/login'], {
      queryParamsHandling: 'preserve',
    });
  }

  // Only same-app paths are accepted so a crafted link cannot redirect elsewhere.
  private returnUrl(): string {
    const url = this.route.snapshot.queryParamMap.get('returnUrl');
    return url && url.startsWith('/') && !url.startsWith('//') && !url.startsWith('/login') && !url.startsWith('/register')
      ? url
      : '/events';
  }
}
