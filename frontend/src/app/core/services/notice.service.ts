import { Injectable, signal } from '@angular/core';

// One-shot message shown after a redirect (e.g. "you do not have access to that page").
@Injectable({ providedIn: 'root' })
export class NoticeService {
  readonly message = signal('');

  show(message: string): void {
    this.message.set(message);
  }

  clear(): void {
    this.message.set('');
  }
}
