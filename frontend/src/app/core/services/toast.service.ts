import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  type: 'success' | 'error';
  text: string;
  leaving: boolean;
}

export const TOAST_DURATION_MS = 3000;
export const TOAST_FADE_MS = 300;

// App-wide transient notifications. Lives in the root injector so toasts survive route changes.
@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);

  private nextId = 1;
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>[]>();

  success(text: string): void {
    this.push('success', text);
  }

  error(text: string): void {
    this.push('error', text);
  }

  dismiss(id: number): void {
    const toast = this.toasts().find((t) => t.id === id);
    if (!toast || toast.leaving) return;
    this.clearTimers(id);
    this.toasts.update((list) => list.map((t) => (t.id === id ? { ...t, leaving: true } : t)));
    this.timers.set(id, [setTimeout(() => this.remove(id), TOAST_FADE_MS)]);
  }

  private push(type: Toast['type'], text: string): void {
    // The same message while it is still showing is the same operation reported twice.
    if (this.toasts().some((t) => t.text === text && t.type === type && !t.leaving)) return;
    const id = this.nextId++;
    this.toasts.update((list) => [...list, { id, type, text, leaving: false }]);
    this.timers.set(id, [setTimeout(() => this.dismiss(id), TOAST_DURATION_MS)]);
  }

  private remove(id: number): void {
    this.clearTimers(id);
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  private clearTimers(id: number): void {
    this.timers.get(id)?.forEach(clearTimeout);
    this.timers.delete(id);
  }
}
